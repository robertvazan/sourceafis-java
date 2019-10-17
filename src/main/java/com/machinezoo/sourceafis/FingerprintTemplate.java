// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;
import javax.imageio.*;
import org.apache.commons.io.*;
import org.slf4j.*;
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
 * Serialization into JSON format is performed by {@link #serialize()} method.
 * JSON template can be deserialized by calling {@link #deserialize(String)}.
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
	 * + other fingerprint properties set in FingerprintImage (only those relevant to matching, so no width/height)
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
	 * + String svg() - maybe, unless there is dedicated visualization library/API
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
	/*
	 * We should drop this indirection once deprecated methods are dropped
	 * and FingerprintTemplate itself becomes immutable.
	 */
	volatile ImmutableTemplate immutable = ImmutableTemplate.empty;
	private static final Logger logger = LoggerFactory.getLogger(FingerprintCompatibility.class);
	/**
	 * Create fingerprint template from fingerprint image.
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
		TemplateBuilder builder = new TemplateBuilder();
		builder.extract(image.matrix, image.dpi);
		immutable = new ImmutableTemplate(builder);
	}
	/**
	 * Deserialize fingerprint template from compressed JSON.
	 * This constructor reads gzip-compressed JSON template produced by {@link #toByteArray()}
	 * and reconstructs an exact copy of the original fingerprint template.
	 * <p>
	 * Templates produced by previous versions of SourceAFIS may fail to deserialize correctly.
	 * Applications should re-extract all templates from original raw images when upgrading SourceAFIS.
	 * 
	 * @param serialized
	 *            serialized fingerprint template in gzip-compressed JSON format produced by {@link #toByteArray()}
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
	FingerprintTemplate(byte[] serialized, boolean foreignToo) {
		try {
			Objects.requireNonNull(serialized);
			byte[] decompressed = Exceptions.wrap().get(() -> {
				try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(serialized))) {
					return IOUtils.toByteArray(gzip);
				}
			});
			String json = new String(decompressed, StandardCharsets.UTF_8);
			TemplateBuilder builder = new TemplateBuilder();
			builder.deserialize(json);
			immutable = new ImmutableTemplate(builder);
		} catch (Throwable ex) {
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
				throw ex;
			}
		}
	}
	/**
	 * Instantiate an empty fingerprint template. This constructor is deprecated.
	 * In the past, it was used together with methods {@link #create(byte[])}, {@link #deserialize(String)},
	 * and {@link #convert(byte[])}, which are all deprecated now.
	 * Use {@link #FingerprintTemplate(FingerprintImage)} and {@link #FingerprintTemplate(byte[])} instead.
	 * 
	 * @see #FingerprintTemplate(FingerprintImage)
	 * @see #FingerprintTemplate(byte[])
	 */
	@Deprecated public FingerprintTemplate() {
	}
	/**
	 * Get the empty template with no biometric data.
	 * Empty template is useful as a fallback to simplify code.
	 * It contains no biometric data and it doesn't match any other template including itself.
	 * There is only one global instance. This method doesn't instantiate any new objects.
	 * 
	 * @return empty template
	 */
	public static FingerprintTemplate empty() {
		return empty;
	}
	private static final FingerprintTemplate empty = new FingerprintTemplate(ImmutableTemplate.empty);
	FingerprintTemplate(ImmutableTemplate immutable) {
		this.immutable = immutable;
	}
	/**
	 * Enable algorithm transparency.
	 * Since {@link FingerprintTransparency} is activated automatically via thread-local variable
	 * in recent versions of SourceAFIS, this method does nothing in current version of SourceAFIS.
	 * It will be removed in some later version.
	 * 
	 * @param transparency
	 *            target {@link FingerprintTransparency} or {@code null} to disable algorithm transparency
	 * @return {@code this} (fluent method)
	 * 
	 * @see FingerprintTransparency
	 */
	@Deprecated public FingerprintTemplate transparency(FingerprintTransparency transparency) {
		return this;
	}
	private double dpi = 500;
	/**
	 * Set DPI (dots per inch) of the fingerprint image.
	 * This is the DPI of the image later passed to {@link #create(byte[])}.
	 * Check your fingerprint reader specification for correct DPI value. Default DPI is 500.
	 * <p>
	 * This method is deprecated. Use {@link FingerprintImage#dpi(double)} and {@link #FingerprintTemplate(FingerprintImage)} instead.
	 * 
	 * @param dpi
	 *            DPI of the fingerprint image, usually around 500
	 * @return {@code this} (fluent method)
	 * 
	 * @see FingerprintImage#dpi(double)
	 * @see #FingerprintTemplate(FingerprintImage)
	 */
	@Deprecated public FingerprintTemplate dpi(double dpi) {
		this.dpi = dpi;
		return this;
	}
	/**
	 * Create fingerprint template from fingerprint image.
	 * The image must contain black fingerprint on white background at the DPI specified by calling {@link #dpi(double)}.
	 * <p>
	 * The image may be in any format commonly used to store fingerprint images, including PNG, JPEG, BMP, TIFF, or WSQ.
	 * SourceAFIS will try to decode the image using Java's {@link ImageIO} (PNG, JPEG, BMP),
	 * <a href="https://commons.apache.org/proper/commons-imaging/">Sanselan</a> library (TIFF),
	 * <a href="https://github.com/kareez/jnbis">JNBIS</a> library (WSQ), and Android's
	 * <a href="https://developer.android.com/reference/android/graphics/Bitmap">Bitmap</a> class (PNG, JPEG, BMP) in this order.
	 * Note that these libraries might not support all versions and variations of the mentioned formats.
	 * <p>
	 * This method replaces any previously added biometric data in this template.
	 * <p>
	 * This method is deprecated. Use {@link FingerprintImage#decode(byte[])} and {@link #FingerprintTemplate(FingerprintImage)} instead.
	 * 
	 * @param image
	 *            fingerprint image in {@link ImageIO}-supported format
	 * @return {@code this} (fluent method)
	 * 
	 * @see FingerprintImage#decode(byte[])
	 * @see #FingerprintTemplate(FingerprintImage)
	 */
	@Deprecated public FingerprintTemplate create(byte[] image) {
		TemplateBuilder builder = new TemplateBuilder();
		builder.extract(new FingerprintImage().decode(image).matrix, dpi);
		immutable = new ImmutableTemplate(builder);
		return this;
	}
	/**
	 * Deserialize fingerprint template from JSON string.
	 * This method does the same thing as {@link #FingerprintTemplate(byte[])} constructor
	 * except it uses plain JSON format produced by {@link #serialize()}.
	 * Use {@link #toByteArray()}} and {@link #FingerprintTemplate(byte[])} instead.
	 * <p>
	 * This method replaces any previously added biometric data in this template.
	 * 
	 * @param json
	 *            serialized fingerprint template in JSON format produced by {@link #serialize()}
	 * @return {@code this} (fluent method)
	 * @throws NullPointerException
	 *             if {@code json} is {@code null}
	 * @throws RuntimeException
	 *             if {@code json} is is not in the correct format or it is corrupted
	 * 
	 * @see #serialize()
	 * @see #FingerprintTemplate(byte[])
	 */
	@Deprecated public FingerprintTemplate deserialize(String json) {
		Objects.requireNonNull(json);
		TemplateBuilder builder = new TemplateBuilder();
		builder.deserialize(json);
		immutable = new ImmutableTemplate(builder);
		return this;
	}
	/**
	 * Serialize fingerprint template as compressed JSON.
	 * Serialized template can be stored in a database or sent over network.
	 * It can be deserialized by calling {@link #FingerprintTemplate(byte[])} constructor.
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
	 * @return serialized fingerprint template in gzip-compressed JSON format
	 * 
	 * @see #FingerprintTemplate(byte[])
	 * @see <a href="https://sourceafis.machinezoo.com/template">Template format</a>
	 */
	public byte[] toByteArray() {
		ImmutableTemplate current = immutable;
		String json = new Gson().toJson(new JsonTemplate(current.size, current.minutiae));
		byte[] uncompressed = json.getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		Exceptions.sneak().run(() -> {
			try (GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
				gzip.write(uncompressed);
			}
		});
		return buffer.toByteArray();
	}
	/**
	 * Serialize fingerprint template to JSON string.
	 * This deprecated method is equivalent to {@link #toByteArray()}
	 * except that the output format is an uncompressed JSON string.
	 * 
	 * @return serialized fingerprint template in JSON format
	 * 
	 * @see #toByteArray()
	 */
	@Deprecated public String serialize() {
		ImmutableTemplate current = immutable;
		return new Gson().toJson(new JsonTemplate(current.size, current.minutiae));
	}
	/**
	 * Import ANSI INCITS 378 or ISO 19794-2 fingerprint template from another fingerprint recognition system.
	 * <p>
	 * This method replaces any previously added biometric data in this template.
	 * <p>
	 * This method is deprecated. Use {@link FingerprintCompatibility#convert(byte[])} instead.
	 * 
	 * @param template
	 *            foreign template to import
	 * @return {@code this} (fluent method)
	 * 
	 * @see #create(byte[])
	 * @see #deserialize(String)
	 * @see #serialize()
	 */
	@Deprecated public FingerprintTemplate convert(byte[] template) {
		immutable = FingerprintCompatibility.convert(template).immutable;
		return this;
	}
}
