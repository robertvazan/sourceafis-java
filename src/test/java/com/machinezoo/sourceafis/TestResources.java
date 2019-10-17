// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.io.*;
import org.apache.commons.io.*;
import com.machinezoo.noexception.*;

class TestResources {
	private static byte[] load(String name) {
		return Exceptions.sneak().get(() -> {
			try (InputStream input = TestResources.class.getResourceAsStream(name)) {
				return IOUtils.toByteArray(input);
			}
		});
	}
	public static byte[] png() {
		return load("probe.png");
	}
	public static byte[] jpeg() {
		return load("probe.jpeg");
	}
	public static byte[] bmp() {
		return load("probe.bmp");
	}
	public static byte[] tiff() {
		return load("probe.tiff");
	}
	public static byte[] originalWsq() {
		return load("wsq-original.wsq");
	}
	public static byte[] convertedWsq() {
		return load("wsq-converted.png");
	}
	public static byte[] probe() {
		return load("probe.png");
	}
	public static byte[] matching() {
		return load("matching.png");
	}
	public static byte[] nonmatching() {
		return load("nonmatching.png");
	}
	public static byte[] probeGray() {
		return load("gray-probe.dat");
	}
	public static byte[] matchingGray() {
		return load("gray-matching.dat");
	}
	public static byte[] nonmatchingGray() {
		return load("gray-nonmatching.dat");
	}
	public static byte[] probeIso() {
		return load("iso-probe.dat");
	}
	public static byte[] matchingIso() {
		return load("iso-matching.dat");
	}
	public static byte[] nonmatchingIso() {
		return load("iso-nonmatching.dat");
	}
}
