package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import java.io.*;
import org.apache.commons.io.*;
import org.junit.*;
import com.machinezoo.noexception.*;

public class FingerprintImageTest {
	private static byte[] load(String name) {
		return Exceptions.sneak().get(() -> {
			try (InputStream input = FingerprintImageTest.class.getResourceAsStream(name)) {
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
	public static byte[] originalWSQ() {
		return load("wsq-original.wsq");
	}
	public static byte[] convertedWSQ() {
		return load("wsq-converted.png");
	}
	@Test public void decodePNG() {
		new FingerprintImage().decode(png());
	}
	private void assertSimilar(DoubleMap map, DoubleMap reference) {
		assertEquals(reference.width, map.width);
		assertEquals(reference.height, map.height);
		double delta = 0, max = -1, min = 1;
		for (int x = 0; x < map.width; ++x) {
			for (int y = 0; y < map.height; ++y) {
				delta += Math.abs(map.get(x, y) - reference.get(x, y));
				max = Math.max(max, map.get(x, y));
				min = Math.min(min, map.get(x, y));
			}
		}
		assertTrue(max > 0.75);
		assertTrue(min < 0.1);
		assertTrue(delta / (map.width * map.height) < 0.01);
	}
	private void assertSimilar(byte[] image, byte[] reference) {
		assertSimilar(new FingerprintImage().decode(image).decoded, new FingerprintImage().decode(reference).decoded);
	}
	@Test public void decodeJPEG() {
		assertSimilar(jpeg(), png());
	}
	@Test public void decodeBMP() {
		assertSimilar(bmp(), png());
	}
	@Test public void decodeTIFF() {
		assertSimilar(tiff(), png());
	}
	@Test public void decodeWSQ() {
		assertSimilar(originalWSQ(), convertedWSQ());
	}
	public static FingerprintImage probe() {
		return new FingerprintImage().decode(load("probe.png"));
	}
	public static FingerprintImage matching() {
		return new FingerprintImage().decode(load("matching.png"));
	}
	public static FingerprintImage nonmatching() {
		return new FingerprintImage().decode(load("nonmatching.png"));
	}
}
