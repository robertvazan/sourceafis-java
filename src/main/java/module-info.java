// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
/**
 * Java implementation of SourceAFIS fingerprint recognition algorithm.
 * See {@link com.machinezoo.sourceafis} package.
 */
module com.machinezoo.sourceafis {
	exports com.machinezoo.sourceafis;
	requires com.machinezoo.noexception;
	requires transitive com.machinezoo.fingerprintio;
	requires java.desktop;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.dataformat.cbor;
	requires com.google.gson;
	requires it.unimi.dsi.fastutil;
	requires org.apache.commons.io;
	requires jnbis;
	requires org.slf4j;
	opens com.machinezoo.sourceafis to com.fasterxml.jackson.databind, com.google.gson;
}
