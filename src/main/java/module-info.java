// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
import com.machinezoo.stagean.*;

/**
 * Java implementation of SourceAFIS fingerprint recognition algorithm.
 * See {@link com.machinezoo.sourceafis} package.
 */
@CodeIssue("Integrate prototype code into public library.")
module com.machinezoo.sourceafis {
	exports com.machinezoo.sourceafis;
	/*
	 * We only need ImageIO from the whole desktop module.
	 */
	requires java.desktop;
	requires com.machinezoo.stagean;
	/*
	 * Transitive, because FingerprintTransparency implements it.
	 */
	requires transitive com.machinezoo.closeablescope;
	/*
	 * Transitive, because we expose ExceptionHandler in the API.
	 */
	requires transitive com.machinezoo.noexception;
	/*
	 * Transitive, because we are using FingerprintIO types in the API.
	 * It's just TemplateFormat at the moment, but it could be expanded with foreign template options in the future.
	 */
	requires transitive com.machinezoo.fingerprintio;
	/*
	 * Needed for setVisibility(PropertyAccessor.FIELD, Visibility.ANY).
	 */
	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.dataformat.cbor;
	/*
	 * Gson is only used by deprecated JSON serialization of templates.
	 */
	requires com.google.gson;
	requires it.unimi.dsi.fastutil;
	requires org.apache.commons.io;
	requires com.github.mhshams.jnbis;
	/*
	 * Serialization needs reflection access.
	 */
	opens com.machinezoo.sourceafis.engine.templates to com.fasterxml.jackson.databind, com.google.gson;
	opens com.machinezoo.sourceafis.engine.primitives to com.fasterxml.jackson.databind, com.google.gson;
	opens com.machinezoo.sourceafis.engine.features to com.fasterxml.jackson.databind, com.google.gson;
	opens com.machinezoo.sourceafis.engine.transparency to com.fasterxml.jackson.databind;
	opens com.machinezoo.sourceafis.engine.matcher to com.fasterxml.jackson.databind;
}
