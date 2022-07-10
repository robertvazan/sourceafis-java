// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.nio.charset.*;
import java.util.*;
import com.machinezoo.fingerprintio.*;
import com.machinezoo.noexception.*;
import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.templates.*;

/**
 * Collection of methods for export and import of foreign fingerprint template formats.
 * Only <a href="https://templates.machinezoo.com/">publicly documented formats</a>
 * implemented in <a href="https://fingerprintio.machinezoo.com/">FingerprintIO</a> are supported,
 * specifically ANSI 378 (2004, 2009, and 2009/Am1) and ISO 19794-2 (2005 and 2011 off-card variants).
 * <p>
 * ANSI and ISO template specs prescribe specific ways to calculate minutia position and angle.
 * They even vary these calculations between versions of the same spec.
 * SourceAFIS has its own algorithms to determine minutia positions and angles.
 * Conversion routines currently don't attempt to compensate for this difference.
 * They just copy minutia positions and angles to the target template without any adjustments.
 * This may result in some loss of accuracy when matching against templates from other sources.
 * <p>
 * Note that the use of these so-called "standard" templates for fingerprint exchange is
 * <a href="https://templates.machinezoo.com/discouraged">strongly discouraged</a>
 * in favor of plain fingerprint images.
 * 
 * @see <a href="https://templates.machinezoo.com/">Fingerprint template formats</a>
 * @see <a href="https://fingerprintio.machinezoo.com/">FingerprintIO</a>
 * @see <a href="https://templates.machinezoo.com/discouraged">Why "standard" templates are a bad idea</a>
 */
public class FingerprintCompatibility {
	static {
		PlatformCheck.run();
	}
	private FingerprintCompatibility() {
	}
	private static String version = new String(PlatformCheck.resource("version.txt"), StandardCharsets.UTF_8).trim();
	/**
	 * Gets version of the currently running SourceAFIS.
	 * This is useful during upgrades when the application has to deal
	 * with possible template incompatibility between versions.
	 * 
	 * @return SourceAFIS version in a three-part 1.2.3 format
	 */
	public static String version() {
		return version;
	}
	/**
	 * Validates and then converts non-native fingerprint template to a list of native SourceAFIS templates.
	 * Several native templates can be returned for one non-native template,
	 * because many non-native template formats can contain multiple fingerprints
	 * while native SourceAFIS templates always contain one fingerprint.
	 * <p>
	 * This method accepts <a href="https://templates.machinezoo.com/">publicly documented</a> template formats
	 * implemented in <a href="https://fingerprintio.machinezoo.com/">FingerprintIO</a> library,
	 * specifically ANSI 378 (2004, 2009, and 2009/Am1) and ISO 19794-2 (2005 and 2011 off-card variants).
	 * <p>
	 * Recoverable parsing exceptions are passed to the provided exception handler.
	 * Use {@link Exceptions#silence()} for permissive parsing and {@link Exceptions#propagate()} for strict parsing.
	 * The former is equivalent to calling {@link #importTemplates(byte[])}.
	 * <p>
	 * If you just need to deserialize native SourceAFIS template,
	 * call {@link FingerprintTemplate#FingerprintTemplate(byte[])} instead.
	 * To create template from fingerprint image,
	 * call {@link FingerprintTemplate#FingerprintTemplate(FingerprintImage)}.
	 * 
	 * @param template
	 *            non-native template in one of the supported formats
	 * @param handler
	 *            exception handler for recoverable parsing exceptions
	 * @return native templates containing fingerprints from the non-native template
	 * @throws NullPointerException
	 *             if {@code template} is {@code null}
	 * @throws TemplateFormatException
	 *             if {@code template} is in an unsupported format or it is corrupted
	 *
	 * @see #importTemplates(byte[])
	 * @see #importTemplate(byte[])
	 * @see #exportTemplates(TemplateFormat, FingerprintTemplate...)
	 * @see <a href="https://fingerprintio.machinezoo.com/">FingerprintIO</a>
	 */
	public static List<FingerprintTemplate> importTemplates(byte[] template, ExceptionHandler handler) {
		Objects.requireNonNull(template);
		try {
			TemplateFormat format = TemplateFormat.identify(template);
			if (format == null || !TemplateCodec.ALL.containsKey(format))
				throw new TemplateFormatException("Unsupported template format.");
			return TemplateCodec.ALL.get(format).decode(template, handler).stream()
				.map(fp -> new FingerprintTemplate(new SearchTemplate(fp)))
				.collect(toList());
		} catch (Throwable ex) {
			/*
			 * If it's none of the known foreign formats, try our own native format
			 * in case native template gets here by accident.
			 */
			try {
				/*
				 * Pass false to FingerprintTemplate constructor to prevent infinite recursion
				 * between FingerprintTemplate and FingerprintCompatibility.
				 */
				new FingerprintTemplate(template, false);
			} catch (Throwable ex2) {
				/*
				 * It's not a native template. Throw the original exception.
				 */
				throw ex;
			}
			throw new TemplateFormatException("Use FingerprintTemplate constructor to parse native templates.");
		}
	}
	/**
	 * Converts non-native fingerprint template to a list of native SourceAFIS templates.
	 * Several native templates can be returned for one non-native template,
	 * because many non-native template formats can contain multiple fingerprints
	 * while native SourceAFIS templates always contain one fingerprint.
	 * <p>
	 * This method accepts <a href="https://templates.machinezoo.com/">publicly documented</a> template formats
	 * implemented in <a href="https://fingerprintio.machinezoo.com/">FingerprintIO</a> library,
	 * specifically ANSI 378 (2004, 2009, and 2009/Am1) and ISO 19794-2 (2005 and 2011 off-card variants).
	 * <p>
	 * Template is parsed permissively. Recoverable errors are ignored.
	 * To customize error handling, call {@link #importTemplates(byte[], ExceptionHandler)}.
	 * <p>
	 * If you just need to deserialize native SourceAFIS template,
	 * call {@link FingerprintTemplate#FingerprintTemplate(byte[])} instead.
	 * To create template from fingerprint image,
	 * call {@link FingerprintTemplate#FingerprintTemplate(FingerprintImage)}.
	 * 
	 * @param template
	 *            non-native template in one of the supported formats
	 * @return native templates containing fingerprints from the non-native template
	 * @throws NullPointerException
	 *             if {@code template} is {@code null}
	 * @throws TemplateFormatException
	 *             if {@code template} is in an unsupported format or it is corrupted
	 *
	 * @see #importTemplates(byte[], ExceptionHandler)
	 * @see #importTemplate(byte[])
	 * @see #exportTemplates(TemplateFormat, FingerprintTemplate...)
	 * @see <a href="https://fingerprintio.machinezoo.com/">FingerprintIO</a>
	 */
	public static List<FingerprintTemplate> importTemplates(byte[] template) {
		return importTemplates(template, Exceptions.silence());
	}
	/**
	 * Converts non-native fingerprint template to native SourceAFIS template.
	 * Single non-native template may contain multiple fingerprints. This method returns the first one.
	 * Call {@link #importTemplates(byte[])} to convert all fingerprints in the template.
	 * <p>
	 * This method accepts <a href="https://templates.machinezoo.com/">publicly documented</a> template formats
	 * implemented in <a href="https://fingerprintio.machinezoo.com/">FingerprintIO</a> library,
	 * specifically ANSI 378 (2004, 2009, and 2009/Am1) and ISO 19794-2 (2005 and 2011 off-card variants).
	 * <p>
	 * Template is parsed permissively. Recoverable errors are ignored.
	 * To customize error handling, call {@link #importTemplates(byte[], ExceptionHandler)}.
	 * <p>
	 * If you just need to deserialize native SourceAFIS template,
	 * call {@link FingerprintTemplate#FingerprintTemplate(byte[])} instead.
	 * To create template from fingerprint image,
	 * call {@link FingerprintTemplate#FingerprintTemplate(FingerprintImage)}.
	 * 
	 * @param template
	 *            non-native template in one of the supported formats
	 * @return converted native template
	 * @throws NullPointerException
	 *             if {@code template} is {@code null}
	 * @throws TemplateFormatException
	 *             if {@code template} is in an unsupported format or it is corrupted
	 * @throws IllegalArgumentException
	 *             if {@code template} contains no fingerprints
	 *
	 * @see FingerprintTemplate#FingerprintTemplate(byte[])
	 * @see #importTemplates(byte[])
	 * @see #exportTemplates(TemplateFormat, FingerprintTemplate...)
	 * @see <a href="https://fingerprintio.machinezoo.com/">FingerprintIO</a>
	 */
	public static FingerprintTemplate importTemplate(byte[] template) {
		Objects.requireNonNull(template);
		return importTemplates(template).stream()
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("No fingerprints found in the template."));
	}
	/**
	 * Converts one or more native templates to non-native template format.
	 * Several native templates can be provided,
	 * because many non-native template formats can encode several fingerprints in one template.
	 * Creating template with zero fingerprints is allowed by some formats.
	 * <p>
	 * This method supports <a href="https://templates.machinezoo.com/">publicly documented</a> template formats
	 * implemented in <a href="https://fingerprintio.machinezoo.com/">FingerprintIO</a> library,
	 * specifically ANSI 378 (2004, 2009, and 2009/Am1) and ISO 19794-2 (2005 and 2011 off-card variants).
	 * To tweak contents of the exported template, deserialize it with FingerprintIO,
	 * perform required changes, and serialize it again with FingerprintIO.
	 * <p>
	 * If you just need to serialize native SourceAFIS template,
	 * call {@link FingerprintTemplate#toByteArray()} instead.
	 * 
	 * @param format
	 *            target non-native template format
	 * @param templates
	 *            list of native SourceAFIS templates to export
	 * @return template in the specified non-native format
	 * @throws NullPointerException
	 *             if {@code format}, {@code templates}, or any of the templates are {@code null}
	 * @throws TemplateFormatException
	 *             if {@code format} is unsupported or export fails for some reason
	 * 
	 * @see FingerprintTemplate#toByteArray()
	 * @see #importTemplates(byte[])
	 * @see <a href="https://fingerprintio.machinezoo.com/">FingerprintIO</a>
	 */
	public static byte[] exportTemplates(TemplateFormat format, FingerprintTemplate... templates) {
		Objects.requireNonNull(format);
		Objects.requireNonNull(templates);
		if (!TemplateCodec.ALL.containsKey(format))
			throw new TemplateFormatException("Unsupported template format.");
		return TemplateCodec.ALL.get(format).encode(Arrays.stream(templates).map(t -> t.inner.features()).collect(toList()));
	}
	/**
	 * @deprecated Use {@link #importTemplates(byte[])} instead.
	 * 
	 * @param template
	 *            foreign template in one of the supported formats
	 * @return native templates containing fingerprints from the foreign template
	 * @throws NullPointerException
	 *             if {@code template} is {@code null}
	 * @throws TemplateFormatException
	 *             if {@code template} is in an unsupported format or it is corrupted
	 */
	@Deprecated
	public static List<FingerprintTemplate> convertAll(byte[] template) {
		return importTemplates(template);
	}
	/**
	 * @deprecated Use {@link #importTemplate(byte[])} instead.
	 * 
	 * @param template
	 *            foreign template in one of the supported formats
	 * @return converted native template
	 * @throws NullPointerException
	 *             if {@code template} is {@code null}
	 * @throws TemplateFormatException
	 *             if {@code template} is in an unsupported format or it is corrupted
	 * @throws IllegalArgumentException
	 *             if {@code template} contains no fingerprints
	 */
	@Deprecated
	public static FingerprintTemplate convert(byte[] template) {
		return importTemplate(template);
	}
	/**
	 * @deprecated Use {@link #exportTemplates(TemplateFormat, FingerprintTemplate...)} instead.
	 * 
	 * @param templates
	 *            list of native SourceAFIS templates to export
	 * @return ANSI 378-2004 template
	 * @throws NullPointerException
	 *             if {@code templates} or any of its items is {@code null}
	 */
	@Deprecated
	public static byte[] toAnsiIncits378v2004(FingerprintTemplate... templates) {
		return exportTemplates(TemplateFormat.ANSI_378_2004, templates);
	}
	/**
	 * @deprecated Use {@link #exportTemplates(TemplateFormat, FingerprintTemplate...)} instead.
	 * 
	 * @param templates
	 *            list of native SourceAFIS templates to export
	 * @return ANSI 378-2009 template
	 * @throws NullPointerException
	 *             if {@code templates} or any of its items is {@code null}
	 */
	@Deprecated
	public static byte[] toAnsiIncits378v2009(FingerprintTemplate... templates) {
		return exportTemplates(TemplateFormat.ANSI_378_2009, templates);
	}
	/**
	 * @deprecated Use {@link #exportTemplates(TemplateFormat, FingerprintTemplate...)} instead.
	 * 
	 * @param templates
	 *            list of native SourceAFIS templates to export
	 * @return ANSI 378-2009/AM1 template
	 * @throws NullPointerException
	 *             if {@code templates} or any of its items is {@code null}
	 */
	@Deprecated
	public static byte[] toAnsiIncits378v2009AM1(FingerprintTemplate... templates) {
		return exportTemplates(TemplateFormat.ANSI_378_2009_AM1, templates);
	}
}
