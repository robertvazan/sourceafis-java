// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.nio.charset.*;
import java.util.*;

/**
 * Collection of methods for export and import of foreign fingerprint template formats.
 * Only <a href="https://templates.machinezoo.com/">publicly documented formats</a> are supported.
 * Three versions of ANSI 378 can be both imported and exported.
 * Limited support for import of ISO 19794-2 is also provided.
 * <p>
 * ANSI and ISO template specs prescribe specific ways to calculate minutia position and angle.
 * They even vary these calculations between versions of the same spec.
 * SourceAFIS has its own algorithms to determine minutia positions and angles.
 * Conversion routines currently don't attempt to compensate for this difference.
 * They just copy minutia positions and angles to the target template without any adjustments.
 * This may result in some loss of accuracy when matching against templates from other sources.
 * <p>
 * Note that the use of these so-called "standard" templates for fingerprint exchange is
 * <a href="https://templates.machinezoo.com/standard-fingerprint-templates-bad-idea">strongly discouraged</a>
 * in favor of plain fingerprint images.
 * 
 * @see <a href="https://templates.machinezoo.com/">Fingerprint template formats</a>
 * @see <a href="https://templates.machinezoo.com/standard-fingerprint-templates-bad-idea">Why "standard" templates are a bad idea</a>
 */
public class FingerprintCompatibility {
	private FingerprintCompatibility() {
	}
	/**
	 * Convert foreign fingerprint template to native SourceAFIS template.
	 * This is a convenience wrapper around {@link #convertAll(byte[])}
	 * that returns the first fingerprint in the template or throws if there are none.
	 * <p>
	 * This method accepts all template formats documented on the
	 * <a href="https://templates.machinezoo.com/">template formats website</a>,
	 * specifically all versions of ANSI 378 and the initial version of ISO 19794-2.
	 * Foreign template can come from feature extractor other than SourceAFIS.
	 * 
	 * @param template
	 *            imported foreign template in one of the supported formats
	 * @return converted native template
	 *
	 * @see #convertAll(byte[])
	 * @see #toAnsiIncits378v2004(FingerprintTemplate...)
	 * @see <a href="https://templates.machinezoo.com/">Supported fingerprint template formats</a>
	 */
	public static FingerprintTemplate convert(byte[] template) {
		return convertAll(template).stream()
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("No fingerprints found in the template"));
	}
	/**
	 * Convert foreign fingerprint template to a list of native SourceAFIS templates.
	 * This method accepts all template formats documented on the
	 * <a href="https://templates.machinezoo.com/">template formats website</a>,
	 * specifically all versions of ANSI 378 and the initial version of ISO 19794-2.
	 * Foreign template can come from feature extractor other than SourceAFIS.
	 * Several native templates can be returned, because many foreign template formats can contain multiple fingerprints.
	 * 
	 * @param template
	 *            imported foreign template in one of the supported formats
	 * @return list of native templates containing fingerprints from the foreign template
	 *
	 * @see #convert(byte[])
	 * @see #toAnsiIncits378v2004(FingerprintTemplate...)
	 * @see <a href="https://templates.machinezoo.com/">Supported fingerprint template formats</a>
	 */
	public static List<FingerprintTemplate> convertAll(byte[] template) {
		/*
		 * If we receive native template here by accident, just deserialize it instead of throwing an exception.
		 */
		if (template.length >= 2 && template[0] == '{' && template[template.length - 1] == '}')
			return Arrays.asList(new FingerprintTemplate().deserialize(new String(template, StandardCharsets.UTF_8)));
		ForeignTemplate foreign = ForeignTemplate.read(template);
		return foreign.fingerprints.stream()
			.map(fingerprint -> {
				TemplateBuilder builder = new TemplateBuilder();
				builder.convert(foreign, fingerprint);
				FingerprintTemplate converted = new FingerprintTemplate();
				converted.immutable = new ImmutableTemplate(builder);
				return converted;
			})
			.collect(toList());
	}
	/**
	 * Convert native fingerprint template to ANSI 378-2004 template.
	 * This method produces fingerprint template conforming to
	 * <a href="https://templates.machinezoo.com/ansi-incits-378-2004">ANSI INCITS 378-2004</a>.
	 * The template can then be used by fingerprint matcher other than SourceAFIS.
	 * Several native templates can be supplied, because ANSI 378 template can contain multiple fingerprints.
	 * 
	 * @param templates
	 *            list of native SourceAFIS templates to export
	 * @return ANSI 378-2004 template
	 * 
	 * @see #toAnsiIncits378v2009(FingerprintTemplate...)
	 * @see #toAnsiIncits378v2009AM1(FingerprintTemplate...)
	 * @see #convertAll(byte[])
	 * @see <a href="https://templates.machinezoo.com/ansi-incits-378-2004">ANSI INCITS 378-2004</a>
	 */
	public static byte[] toAnsiIncits378v2004(FingerprintTemplate... templates) {
		ForeignTemplate foreign = new ForeignTemplate(templates);
		foreign.format = ForeignFormat.ANSI_378_2004;
		return foreign.write();
	}
	/**
	 * Convert native fingerprint template to ANSI 378-2009 template.
	 * This method produces fingerprint template conforming to
	 * <a href="https://templates.machinezoo.com/ansi-incits-378-2009-r2014">ANSI INCITS 378-2009[R2014]</a>.
	 * The template can then be used by fingerprint matcher other than SourceAFIS.
	 * Several native templates can be supplied, because ANSI 378 template can contain multiple fingerprints.
	 * 
	 * @param templates
	 *            list of native SourceAFIS templates to export
	 * @return ANSI 378-2009 template
	 * 
	 * @see #toAnsiIncits378v2004(FingerprintTemplate...)
	 * @see #toAnsiIncits378v2009AM1(FingerprintTemplate...)
	 * @see #convertAll(byte[])
	 * @see <a href="https://templates.machinezoo.com/ansi-incits-378-2009-r2014">ANSI INCITS 378-2009[R2014]</a>
	 */
	public static byte[] toAnsiIncits378v2009(FingerprintTemplate... templates) {
		ForeignTemplate foreign = new ForeignTemplate(templates);
		foreign.format = ForeignFormat.ANSI_378_2009;
		return foreign.write();
	}
	/**
	 * Convert native fingerprint template to ANSI 378-2009/AM1 template.
	 * This method produces fingerprint template conforming to
	 * <a href="https://templates.machinezoo.com/ansi-incits-378-2009-am1-2010-r2015">ANSI INCITS 378:2009/AM 1:2010[R2015]</a>.
	 * The template can then be used by fingerprint matcher other than SourceAFIS.
	 * Several native templates can be supplied, because ANSI 378 template can contain multiple fingerprints.
	 * 
	 * @param templates
	 *            list of native SourceAFIS templates to export
	 * @return ANSI 378-2009/AM1 template
	 * 
	 * @see #toAnsiIncits378v2004(FingerprintTemplate...)
	 * @see #toAnsiIncits378v2009(FingerprintTemplate...)
	 * @see #convertAll(byte[])
	 * @see <a href="https://templates.machinezoo.com/ansi-incits-378-2009-am1-2010-r2015">ANSI INCITS 378:2009/AM 1:2010[R2015]</a>
	 */
	public static byte[] toAnsiIncits378v2009AM1(FingerprintTemplate... templates) {
		ForeignTemplate foreign = new ForeignTemplate(templates);
		foreign.format = ForeignFormat.ANSI_378_2009_AM1;
		return foreign.write();
	}
}
