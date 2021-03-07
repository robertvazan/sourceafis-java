// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.util.*;
import javax.imageio.*;
import org.slf4j.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.cbor.*;
import com.google.gson.*;
import com.machinezoo.noexception.*;

/**
 * Biometric description of a fingerprint suitable for efficient matching.
 * Fingerprint template holds high-level fingerprint features, specifically ridge endings and bifurcations (together called minutiae).
 * Original image is not preserved in the fingerprint template and there is no way to reconstruct the original fingerprint from its template.
 * <p>
 * {@link FingerprintImage} can be converted to template by calling {@link #FingerprintTemplate(FingerprintImage)} constructor.
 * <p>
 * Since image processing is expensive, applications should cache serialized templates.
 * Serialization into JSON format is performed by {@link #toByteArray()} method.
 * JSON template can be deserialized by calling {@link #FingerprintTemplate(byte[])}.
 * on an empty fingerprint template instantiated with {@link #FingerprintTemplate()} constructor.
 * <p>
 * Matching is performed by constructing {@link FingerprintMatcher},
 * passing probe fingerprint to its {@link FingerprintMatcher#index(FingerprintTemplate)} method,
 * and then passing candidate fingerprints to its {@link FingerprintMatcher#match(FingerprintTemplate)} method.
 * <p>
 * {@code FingerprintTemplate} contains two kinds of data: fingerprint features and search data structures.
 * Search data structures speed up matching at the cost of some RAM.
 * Only fingerprint features are serialized. Search data structures are recomputed after every deserialization.
 * 
 * @see <a href="https://sourceafis.machinezoo.com/">SourceAFIS overview</a>
 * @see FingerprintImage
 * @see FingerprintMatcher
 */
public class FingerprintTemplate {
	/*
	 * API roadmap:
	 * + FingerprintTemplate(FingerprintImage, FingerprintTemplateOptions)
	 * + double surface() - in metric units
	 * + FingerprintPosition position()
	 * + other fingerprint properties set in FingerprintImageOptions (only those relevant to matching, so no width/height for example)
	 * + FingerprintModel model()
	 * + FingerprintTemplate(FingerprintModel)
	 * + FingerprintTemplate narrow(FingerprintTemplateOptions) - for reducing RAM usage by dropping features
	 * + byte[] pack(int limit) - for producing super-compact templates (even under 100 bytes)
	 * + FingerprintTemplate unpack(byte[] packed)
	 * 
	 * FingerprintTemplateOptions:
	 * + featureX(boolean) - enable/disable production of expensive fingerprint features
	 * + parallelize(boolean)
	 * + cpu(long) - limit on CPU cycles consumed (approximate)
	 * 
	 * FingerprintModel:
	 * = editing-optimized fingerprint representation as opposed to matching- and serialization-optimized FingerprintTemplate
	 * = to be used in forensics and other settings for fingerprint editing
	 * - no DPI, all values in metric units
	 * + double width/height()
	 * + List<FingerprintMinutia> minutiae() - mutable list of mutable minutiae
	 * + all properties exposed by FingerprintTemplate
	 * + setters for everything
	 * 
	 * FingerprintMinutia:
	 * + double x/y()
	 * + double direction()
	 * + also setters
	 * 
	 * FingerprintFusion:
	 * + add(FingerprintTemplate)
	 * + FingerprintTemplate fuse()
	 */
	static {
		PlatformCheck.run();
	}
	/*
	 * We should drop this indirection once deprecated methods are dropped
	 * and FingerprintTemplate itself becomes immutable.
	 */
	volatile ImmutableTemplate immutable = ImmutableTemplate.EMPTY;
	private static final Logger logger = LoggerFactory.getLogger(FingerprintCompatibility.class);
	/**
	 * Creates fingerprint template from fingerprint image.
	 * <p>
	 * This constructor runs an expensive feature extractor algorithm,
	 * which analyzes the image and collects identifiable biometric features from it.
	 * 
	 * @param image
	 *            fingerprint image to process
	 * @throws NullPointerException
	 *             if {@code image} is {@code null} or image data in it was not set
	 */
	public FingerprintTemplate(FingerprintImage image) {
		Objects.requireNonNull(image);
		Objects.requireNonNull(image.matrix);
		immutable = new ImmutableTemplate(FeatureExtractor.extract(image.matrix, image.dpi));
	}
	/**
	 * Deserializes fingerprint template from byte array.
	 * This constructor reads <a href="https://cbor.io/">CBOR</a>-encoded template produced by {@link #toByteArray()}
	 * and reconstructs an exact copy of the original fingerprint template.
	 * <p>
	 * Templates produced by previous versions of SourceAFIS may fail to deserialize correctly.
	 * Applications should re-extract all templates from original images when upgrading SourceAFIS.
	 * 
	 * @param serialized
	 *            serialized fingerprint template in <a href="https://cbor.io/">CBOR</a> format produced by {@link #toByteArray()}
	 * @throws NullPointerException
	 *             if {@code serialized} is {@code null}
	 * @throws RuntimeException
	 *             if {@code serialized} is is not in the correct format or it is corrupted
	 * 
	 * @see #toByteArray()
	 * @see <a href="https://sourceafis.machinezoo.com/template">Template format</a>
	 * @see FingerprintImage#decode(byte[])
	 * @see FingerprintCompatibility#convert(byte[])
	 */
	public FingerprintTemplate(byte[] serialized) {
		this(serialized, true);
	}
	private static final ObjectMapper mapper = new ObjectMapper(new CBORFactory())
		.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
	FingerprintTemplate(byte[] serialized, boolean foreignToo) {
		try {
			Objects.requireNonNull(serialized);
			PersistentTemplate persistent = mapper.readValue(serialized, PersistentTemplate.class);
			persistent.validate();
			immutable = new ImmutableTemplate(persistent.mutable());
		} catch (Throwable ex) {
			if (!foreignToo)
				throw new IllegalArgumentException("This is not a valid SourceAFIS template.", ex);
			try {
				FingerprintTemplate converted = FingerprintCompatibility.convert(serialized);
				immutable = converted.immutable;
				/*
				 * It is an error to pass foreign template here, so at least log a warning.
				 */
				logger.warn("Template in foreign format was passed to FingerprintTemplate constructor. It was accepted, but FingerprintCompatibility.convert() should be used instead.");
			} catch (Throwable ex2) {
				/*
				 * Throw the original exception. We don't want to hide it with exception from this fallback.
				 */
				throw new IllegalArgumentException("This is neither a valid SourceAFIS template nor any supported foreign template format.", ex);
			}
		}
	}
	/**
	 * @deprecated Use one of the constructors that take parameters to create fully initialized template instead.
	 * 
	 * @see #FingerprintTemplate(FingerprintImage)
	 * @see #FingerprintTemplate(byte[])
	 */
	@Deprecated
	public FingerprintTemplate() {
	}
	/**
	 * Gets the empty template with no biometric data.
	 * Empty template is useful as a fallback to simplify code.
	 * It contains no biometric data and it doesn't match any other template including itself.
	 * There is only one global instance. This method doesn't instantiate any new objects.
	 * 
	 * @return empty template
	 */
	public static FingerprintTemplate empty() {
		return EMPTY;
	}
	private static final FingerprintTemplate EMPTY = new FingerprintTemplate(ImmutableTemplate.EMPTY);
	FingerprintTemplate(ImmutableTemplate immutable) {
		this.immutable = immutable;
	}
	/**
	 * @deprecated Use thread-local instance of {@link FingerprintTransparency} instead.
	 * 
	 * @param transparency
	 *            target {@link FingerprintTransparency} or {@code null} to disable algorithm transparency
	 * @return {@code this} (fluent method)
	 * 
	 * @see FingerprintTransparency
	 */
	@Deprecated
	public FingerprintTemplate transparency(FingerprintTransparency transparency) {
		return this;
	}
	private double dpi = 500;
	/**
	 * @deprecated Set DPI via {@link FingerprintImageOptions}{@link #dpi(double)} instead.
	 * 
	 * @param dpi
	 *            DPI of the fingerprint image, usually around 500
	 * @return {@code this} (fluent method)
	 * 
	 * @see FingerprintImageOptions#dpi(double)
	 */
	@Deprecated
	public FingerprintTemplate dpi(double dpi) {
		this.dpi = dpi;
		return this;
	}
	/**
	 * @deprecated Use {@link #FingerprintTemplate(FingerprintImage)} constructor to create template from image.
	 * 
	 * @param image
	 *            fingerprint image in {@link ImageIO}-supported format
	 * @return {@code this} (fluent method)
	 * 
	 * @see #FingerprintTemplate(FingerprintImage)
	 */
	@Deprecated
	public FingerprintTemplate create(byte[] image) {
		immutable = new ImmutableTemplate(FeatureExtractor.extract(new FingerprintImage(image).matrix, dpi));
		return this;
	}
	/**
	 * @deprecated Use {@link #FingerprintTemplate(byte[])} constructor to deserialize the template.
	 * 
	 * @param json
	 *            serialized fingerprint template in JSON format produced by {@link #serialize()}
	 * @return {@code this} (fluent method)
	 * @throws NullPointerException
	 *             if {@code json} is {@code null}
	 * @throws RuntimeException
	 *             if {@code json} is is not in the correct format or it is corrupted
	 * 
	 * @see #FingerprintTemplate(byte[])
	 */
	@Deprecated
	public FingerprintTemplate deserialize(String json) {
		Objects.requireNonNull(json);
		PersistentTemplate persistent = new Gson().fromJson(json, PersistentTemplate.class);
		persistent.validate();
		immutable = new ImmutableTemplate(persistent.mutable());
		return this;
	}
	/**
	 * Serializes fingerprint template into byte array.
	 * Serialized template can be stored in a database or sent over network.
	 * It can be then deserialized by calling {@link #FingerprintTemplate(byte[])} constructor.
	 * Persisting templates alongside fingerprint images allows applications to start faster,
	 * because template deserialization is more than 100x faster than re-extraction from fingerprint image.
	 * <p>
	 * Serialized template excludes search structures that {@code FingerprintTemplate} keeps to speed up matching.
	 * Serialized template is therefore much smaller than in-memory {@code FingerprintTemplate}.
	 * <p>
	 * Serialization format can change with every SourceAFIS version. There is no backward compatibility of templates.
	 * Applications should preserve raw fingerprint images, so that templates can be re-extracted after SourceAFIS upgrade.
	 * Template format for current version of SourceAFIS is
	 * <a href="https://sourceafis.machinezoo.com/template">documented on SourceAFIS website</a>.
	 * 
	 * @return serialized fingerprint template in <a href="https://cbor.io/">CBOR</a> format
	 * 
	 * @see #FingerprintTemplate(byte[])
	 * @see <a href="https://sourceafis.machinezoo.com/template">Template format</a>
	 */
	public byte[] toByteArray() {
		PersistentTemplate persistent = new PersistentTemplate(immutable.mutable());
		return Exceptions.wrap().get(() -> mapper.writeValueAsBytes(persistent));
	}
	/**
	 * @deprecated Use {@link #toByteArray()} to serialize the template.
	 * 
	 * @return serialized fingerprint template in JSON format
	 * 
	 * @see #toByteArray()
	 */
	@Deprecated
	public String serialize() {
		return new Gson().toJson(new PersistentTemplate(immutable.mutable()));
	}
	/**
	 * @deprecated Use {@link FingerprintCompatibility} methods to import other template formats.
	 * 
	 * @param template
	 *            foreign template to import
	 * @return {@code this} (fluent method)
	 * 
	 * @see FingerprintCompatibility#convert(byte[])
	 */
	@Deprecated
	public FingerprintTemplate convert(byte[] template) {
		immutable = FingerprintCompatibility.convert(template).immutable;
		return this;
	}
}
