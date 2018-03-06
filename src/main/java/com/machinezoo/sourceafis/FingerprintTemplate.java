// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import javax.imageio.*;
import com.google.gson.*;

/**
 * Biometric description of a fingerprint suitable for efficient matching.
 * Fingerprint template holds high-level fingerprint features, specifically ridge endings and bifurcations (minutiae).
 * Original image is not preserved in the fingerprint template and there is no way to reconstruct the original fingerprint from its template.
 * <p>
 * In order to create fingerprint template, first instantiate it with {@link #FingerprintTemplate()}
 * and then initialized it by calling {@link #create(byte[], double)}, {@link #deserialize(String)}, or {@link #convert(byte[])}.
 * Most commonly used method {@link #create(byte[], double)} creates fingerprint template from fingerprint image.
 * <p>
 * Since image processing is expensive, applications should cache serialized templates.
 * Serialization is performed by {@link #serialize()} and deserialization by {@link #deserialize(String)}.
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
	volatile ImmutableTemplate immutable = ImmutableTemplate.empty;
	/**
	 * Instantiate an empty fingerprint template.
	 * Empty template represents fingerprint with no features that does not match any other fingerprint.
	 * In order for the template to be useful, it must be first initialized by calling
	 * {@link #create(byte[], double)}, {@link #deserialize(String)}, or {@link #convert(byte[])}.
	 * Only one initializing method is usually called for every {@code FingerprintTemplate}.
	 */
	public FingerprintTemplate() {
	}
	/**
	 * Create fingerprint template from fingerprint image.
	 * This method initializes the {@code FingerprintTemplate} and makes it ready for use.
	 * Image must contain black fingerprint on white background with the specified DPI (dots per inch).
	 * Check your fingerprint reader specification for correct DPI value.
	 * All image formats supported by Java's {@link ImageIO} are accepted, for example JPEG, PNG, or BMP,
	 * 
	 * @param image
	 *            fingerprint image in {@link ImageIO}-supported format
	 * @param dpi
	 *            DPI of the image, usually around 500
	 * @return {@code this} (fluent method)
	 * 
	 * @see #FingerprintTemplate(byte[])
	 */
	public FingerprintTemplate create(byte[] image, double dpi) {
		TemplateBuilder builder = new TemplateBuilder();
		builder.transparency = FingerprintTransparency.current();
		builder.extract(image, dpi);
		immutable = new ImmutableTemplate(builder);
		return this;
	}
	/**
	 * Deserialize fingerprint template from JSON string.
	 * This method initializes the {@code FingerprintTemplate} and makes it ready for use.
	 * It reads JSON string produced by {@link #serialize()} to reconstruct exact copy of the original fingerprint template.
	 * Templates produced by previous versions of SourceAFIS may fail to deserialize correctly.
	 * Applications should re-extract all templates from original raw images when upgrading SourceAFIS.
	 * 
	 * @param json
	 *            serialized fingerprint template in JSON format produced by {@link #serialize()}
	 * @return {@code this} (fluent method)
	 * 
	 * @see #serialize()
	 */
	public FingerprintTemplate deserialize(String json) {
		TemplateBuilder builder = new TemplateBuilder();
		builder.transparency = FingerprintTransparency.current();
		builder.deserialize(json);
		immutable = new ImmutableTemplate(builder);
		return this;
	}
	/**
	 * Serialize fingerprint template to JSON string.
	 * Serialized template can be stored in database or sent over network.
	 * It can be deserialized by calling {@link #deserialize(String)}.
	 * Persisting templates alongside fingerprint images allows applications to start faster,
	 * because template deserialization is more than 100x faster than re-extraction from fingerprint image.
	 * <p>
	 * Serialized template excludes search structures that {@code FingerprintTemplate} keeps to speed up matching.
	 * Serialized template is therefore much smaller than in-memory {@code FingerprintTemplate}.
	 * <p>
	 * Serialization format can change with every SourceAFIS version. There is no backward compatibility of templates.
	 * Applications should preserve raw fingerprint images, so that templates can be re-extracted after SourceAFIS upgrade.
	 * 
	 * @return serialized fingerprint template in JSON format
	 * 
	 * @see #deserialize(String)
	 */
	public String serialize() {
		ImmutableTemplate current = immutable;
		return new Gson().toJson(new JsonTemplate(current.size, current.minutiae));
	}
	/**
	 * Import ISO 19794-2 fingerprint template from another fingerprint recognition system.
	 * This method initializes the {@code FingerprintTemplate} and makes it ready for use.
	 * It can import biometric data from ISO 19794-2 templates,
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
	 * Application developers are encouraged to collect, store, and transfer fingerprints as images.
	 * Besides compatibility and simplicity this brings,
	 * use of images allows SourceAFIS to co-tune its feature extractor and matcher for higher accuracy.
	 * 
	 * @param iso
	 *            ISO 19794-2 template to import
	 * @return {@code this} (fluent method)
	 * 
	 * @see #create(byte[], double)
	 * @see #deserialize(String)
	 * @see #serialize()
	 */
	public FingerprintTemplate convert(byte[] iso) {
		TemplateBuilder builder = new TemplateBuilder();
		builder.transparency = FingerprintTransparency.current();
		builder.convert(iso);
		immutable = new ImmutableTemplate(builder);
		return this;
	}
}
