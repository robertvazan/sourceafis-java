// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.util.*;
import javax.imageio.*;
import com.google.gson.*;

/**
 * Biometric description of a fingerprint suitable for efficient matching.
 * Fingerprint template holds high-level fingerprint features, specifically ridge endings and bifurcations (together called minutiae).
 * Original image is not preserved in the fingerprint template and there is no way to reconstruct the original fingerprint from its template.
 * <p>
 * Fingerprint image can be converted to template by calling {@link #create(byte[])} method
 * on an empty fingerprint template instantiated with {@link #FingerprintTemplate()} constructor.
 * Image DPI may be specified first by calling {@link #dpi(double)}.
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
 * @see FingerprintMatcher
 */
public class FingerprintTemplate {
	private double dpi = 500;
	private FingerprintTransparency transparency = FingerprintTransparency.none;
	volatile ImmutableTemplate immutable = ImmutableTemplate.empty;
	/**
	 * Instantiate an empty fingerprint template.
	 * Empty template represents fingerprint with no features that does not match any other fingerprint (not even itself).
	 * You can then call {@link #create(byte[])} or {@link #deserialize(String)}
	 * to actually fill the template with useful biometric data.
	 */
	public FingerprintTemplate() {
	}
	/**
	 * Enable algorithm transparency.
	 * Subsequent operations on this template will report intermediate data structures created by the algorithm
	 * to the provided {@link FingerprintTransparency} instance.
	 * 
	 * @param transparency
	 *            target {@link FingerprintTransparency} or {@code null} to disable algorithm transparency
	 * @return {@code this} (fluent method)
	 * 
	 * @see FingerprintTransparency
	 */
	public FingerprintTemplate transparency(FingerprintTransparency transparency) {
		this.transparency = Optional.ofNullable(transparency).orElse(FingerprintTransparency.none);
		return this;
	}
	/**
	 * Set DPI (dots per inch) of the fingerprint image.
	 * This is the DPI of the image later passed to {@link #create(byte[])}.
	 * Check your fingerprint reader specification for correct DPI value. Default DPI is 500.
	 * 
	 * @param dpi
	 *            DPI of the fingerprint image, usually around 500
	 * @return {@code this} (fluent method)
	 * 
	 * @see #create(byte[])
	 */
	public FingerprintTemplate dpi(double dpi) {
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
	 * 
	 * @param image
	 *            fingerprint image in {@link ImageIO}-supported format
	 * @return {@code this} (fluent method)
	 * 
	 * @see #dpi(double)
	 */
	public FingerprintTemplate create(byte[] image) {
		TemplateBuilder builder = new TemplateBuilder();
		builder.transparency = transparency;
		builder.extract(image, dpi);
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
		builder.transparency = transparency;
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
	 * This method is deprecated. Use {@link FingerprintCompatibility#convert(byte[])} instead.
	 * <p>
	 * This method replaces any previously added biometric data in this template.
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
