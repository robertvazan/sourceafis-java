// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import javax.imageio.*;
import com.google.gson.*;

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
	 * We should drop this indirection once deprecated methods are dropped
	 * and FingerprintTemplate itself becomes immutable.
	 */
	volatile ImmutableTemplate immutable = ImmutableTemplate.empty;
	/**
	 * Create fingerprint template from fingerprint image.
	 * <p>
	 * This constructor runs an expensive feature extractor algorithm,
	 * which analyzes the image and collects identifiable biometric features from it.
	 * 
	 * @param image
	 *            fingerprint image to process
	 */
	public FingerprintTemplate(FingerprintImage image) {
		TemplateBuilder builder = new TemplateBuilder();
		builder.extract(image.decoded, image.dpi);
		immutable = new ImmutableTemplate(builder);
	}
	/**
	 * Instantiate an empty fingerprint template.
	 * Empty template represents fingerprint with no features that does not match any other fingerprint (not even itself).
	 * You can then call {@link #create(byte[])} or {@link #deserialize(String)}
	 * to actually fill the template with useful biometric data.
	 * <p>
	 * This constructor is largely deprecated. Use {@link #FingerprintTemplate(FingerprintImage)} instead.
	 * This constructor should be only used with {@link #deserialize(String)} method.
	 * 
	 * @see #FingerprintTemplate(FingerprintImage)
	 */
	public FingerprintTemplate() {
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
		builder.extract(ImageDecoder.toDoubleMap(image), dpi);
		immutable = new ImmutableTemplate(builder);
		return this;
	}
	/**
	 * Deserialize fingerprint template from JSON string.
	 * This method reads JSON string produced by {@link #serialize()} to reconstruct an exact copy of the original fingerprint template.
	 * <p>
	 * Templates produced by previous versions of SourceAFIS may fail to deserialize correctly.
	 * Applications should re-extract all templates from original raw images when upgrading SourceAFIS.
	 * <p>
	 * This method replaces any previously added biometric data in this template.
	 * 
	 * @param json
	 *            serialized fingerprint template in JSON format produced by {@link #serialize()}
	 * @return {@code this} (fluent method)
	 * 
	 * @see #serialize()
	 * @see <a href="https://sourceafis.machinezoo.com/template">Template format</a>
	 */
	public FingerprintTemplate deserialize(String json) {
		TemplateBuilder builder = new TemplateBuilder();
		builder.deserialize(json);
		immutable = new ImmutableTemplate(builder);
		return this;
	}
	/**
	 * Serialize fingerprint template to JSON string.
	 * Serialized template can be stored in a database or sent over network.
	 * It can be deserialized by calling {@link #deserialize(String)}.
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
	 * @return serialized fingerprint template in JSON format
	 * 
	 * @see #deserialize(String)
	 * @see <a href="https://sourceafis.machinezoo.com/template">Template format</a>
	 */
	public String serialize() {
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
