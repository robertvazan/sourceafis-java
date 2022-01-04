// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
/**
 * Java implementation of SourceAFIS fingerprint recognition algorithm.
 * See {@link com.machinezoo.sourceafis} package.
 */
module com.machinezoo.sourceafis {
	exports com.machinezoo.sourceafis;
	/*
	 * We only need ImageIO from the whole desktop module.
	 */
	requires java.desktop;
	requires com.machinezoo.noexception;
	/*
	 * Transitive, because we are using FingerprintIO types in the API.
	 * It's just TemplateFormat at the moment, but it could be expanded with foreign template options in the future.
	 */
	requires transitive com.machinezoo.fingerprintio;
	/*
	 * SLF4J is pulled in transitively via noexception, but the transitive dependency will be removed in future versions of noexception.
	 */
	requires org.slf4j;
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
	/*
	 * JNBIS is not yet packaged as a module.
	 * https://github.com/mhshams/jnbis/issues/32
	 */
	requires jnbis;
	/*
	 * Serialization needs reflection access.
	 */
	opens com.machinezoo.sourceafis to com.fasterxml.jackson.databind, com.google.gson;
}
