// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import javax.imageio.*;
import com.google.gson.*;

/**
 * Biometric description of a fingerprint suitable for efficient matching.
 * Fingerprint template holds high-level fingerprint features, specifically ridge endings and bifurcations (minutiae).
 * Original image is not preserved in the fingerprint template and there is no way to reconstruct the original fingerprint from its template.
 * <p>
 * Fingerprint template can be created from fingerprint image by calling {@link #FingerprintTemplate(byte[], double)}.
 * Since image processing is expensive, applications should cache serialized templates.
 * Serialization is performed by {@link #toJson()} and deserialization by {@link #fromJson(String)}.
 * <p>
 * Matching is performed by constructing {@link FingerprintMatcher} and calling its {@link FingerprintMatcher#match(FingerprintTemplate)} method.
 * <p>
 * {@code FingerprintTemplate} contains two kinds of data: fingerprint features and search data structures.
 * Search data structures speed up matching at the cost of some RAM.
 * Only fingerprint features are serialized. Search data structures are recomputed after every deserialization.
 * 
 * @see <a href="https://sourceafis.machinezoo.com/">SourceAFIS overview</a>
 * @see FingerprintMatcher
 */
public class FingerprintTemplate {
	volatile ImmutableTemplate immutable;
	/**
	 * Create fingerprint template from raw fingerprint image.
	 * Image must contain black fingerprint on white background with the specified DPI (dots per inch).
	 * Check your fingerprint reader specification for correct DPI value.
	 * All image formats supported by Java's {@link ImageIO} are accepted, for example JPEG, PNG, or BMP,
	 * 
	 * @param image
	 *            fingerprint image in {@link ImageIO}-supported format
	 * @param dpi
	 *            DPI of the image, usually around 500
	 * 
	 * @see #FingerprintTemplate(byte[])
	 */
	public FingerprintTemplate(byte[] image, double dpi) {
		TemplateBuilder builder = new TemplateBuilder();
		builder.transparency = FingerprintTransparency.current();
		builder.extract(image, dpi);
		immutable = new ImmutableTemplate(builder); 
	}
	/**
	 * Deserialize fingerprint template from JSON string.
	 * This constructor reads JSON string produced by {@link #toJson()} to reconstruct exact copy of the original fingerprint template.
	 * Templates produced by previous versions of SourceAFIS may fail to deserialize correctly.
	 * Applications should re-extract all templates from original raw images when upgrading SourceAFIS.
	 * 
	 * @param json
	 *            serialized fingerprint template in JSON format produced by {@link #toJson()}
	 * @return deserialized fingerprint template
	 * 
	 * @see #toJson()
	 */
	public static FingerprintTemplate fromJson(String json) {
		return new FingerprintTemplate(json);
	}
	private FingerprintTemplate(String json) {
		TemplateBuilder builder = new TemplateBuilder();
		builder.transparency = FingerprintTransparency.current();
		builder.deserialize(json);
		immutable = new ImmutableTemplate(builder); 
	}
	/**
	 * Serialize fingerprint template to JSON string.
	 * Serialized template can be stored in database or sent over network.
	 * It can be deserialized by calling {@link #fromJson(String)}.
	 * Persisting templates alongside fingerprint images allows applications to start faster,
	 * because template deserialization is more than 100x faster than re-extraction from raw image.
	 * <p>
	 * Serialized template excludes search structures that {@code FingerprintTemplate} keeps to speed up matching.
	 * Serialized template is therefore much smaller than in-memory {@code FingerprintTemplate}.
	 * <p>
	 * Serialization format can change with every SourceAFIS version. There is no backward compatibility of templates.
	 * Applications should preserve raw fingerprint images, so that templates can be re-extracted after SourceAFIS upgrade.
	 * 
	 * @return serialized fingerprint template in JSON format
	 * 
	 * @see #fromJson(String)
	 */
	public String toJson() {
		ImmutableTemplate current = immutable;
		return new Gson().toJson(new JsonTemplate(current.size, current.minutiae));
	}
	/**
	 * Import ISO 19794-2 fingerprint template from another fingerprint recognition system.
	 * This method can import biometric data from ISO 19794-2 templates,
	 * which carry fingerprint features (endings and bifurcations) without the original image.
	 * <p>
	 * This method is written for ISO 19794-2:2005, but it should be able to handle ISO 19794-2:2011 templates.
	 * If you believe you have a conforming template, but this method doesn't accept it, mail the template in for analysis.
	 * No other fingerprint template formats are currently supported.
	 * <p>
	 * Note that the use of ISO 19794-2 templates is strongly discouraged
	 * and support for the format might be removed in future releases.
	 * This is because ISO is very unfriendly to opensource developers,
	 * Its "standards" are only available for a high fee and with no redistribution rights.
	 * There is only one truly open and widely used fingerprint exchange format: fingerprint images.
	 * Application developers are encouraged to collect, store, and transfer fingerprints as raw images.
	 * Besides compatibility and simplicity this brings,
	 * use of raw images allows SourceAFIS to co-tune its feature extractor and matcher for higher accuracy.
	 * 
	 * @param iso
	 *            ISO 19794-2 template to import
	 * @return converted fingerprint template
	 * 
	 * @see #FingerprintTemplate(byte[], double)
	 * @see #fromJson(String)
	 * @see #toJson()
	 */
	public static FingerprintTemplate convert(byte[] iso) {
		return new FingerprintTemplate(iso);
	}
	private FingerprintTemplate(byte[] iso) {
		TemplateBuilder builder = new TemplateBuilder();
		builder.transparency = FingerprintTransparency.current();
		builder.convert(iso);
		immutable = new ImmutableTemplate(builder); 
	}
}
