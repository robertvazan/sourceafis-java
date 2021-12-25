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
	requires transitive com.machinezoo.fingerprintio;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.dataformat.cbor;
	/*
	 * Gson is only used by deprecated JSON serialization of templates.
	 */
	requires com.google.gson;
	requires it.unimi.dsi.fastutil;
	requires org.apache.commons.io;
	/*
	 * JNBIS is not packaged as module yet.
	 */
	requires jnbis;
	/*
	 * Serialization needs reflection access.
	 */
	opens com.machinezoo.sourceafis to com.fasterxml.jackson.databind, com.google.gson;
}
