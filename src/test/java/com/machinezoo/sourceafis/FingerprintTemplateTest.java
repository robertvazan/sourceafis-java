package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import java.io.*;
import org.apache.commons.io.*;
import org.junit.*;
import com.machinezoo.sourceafis.models.*;
import lombok.*;

public class FingerprintTemplateTest {
	public static FingerprintTemplate probe() {
		return new FingerprintTemplate(load("probe.png"));
	}
	public static FingerprintTemplate matching() {
		return new FingerprintTemplate(load("matching.png"));
	}
	public static FingerprintTemplate nonmatching() {
		return new FingerprintTemplate(load("nonmatching.png"));
	}
	@Test public void constructor() {
		new FingerprintTemplate(load("probe.png"));
	}
	@Test public void readImage_png() {
		validate(FingerprintTemplate.readImage(load("probe.png")));
	}
	@Test public void readImage_jpeg() {
		validate(FingerprintTemplate.readImage(load("probe.jpeg")));
	}
	@Test public void readImage_bmp() {
		validate(FingerprintTemplate.readImage(load("probe.bmp")));
	}
	@Test public void readImage_tiff() {
		validate(FingerprintTemplate.readImage(load("probe.tiff")));
	}
	private void validate(DoubleMap map) {
		assertEquals(388, map.width);
		assertEquals(374, map.height);
		DoubleMap reference = FingerprintTemplate.readImage(load("probe.png"));
		double delta = 0, max = -1, min = 1;
		for (int x = 0; x < map.width; ++x) {
			for (int y = 0; y < map.height; ++y) {
				delta += Math.abs(map.get(x, y) - reference.get(x, y));
				max = Math.max(max, map.get(x, y));
				min = Math.min(min, map.get(x, y));
			}
		}
		assertTrue(max > 0.9);
		assertTrue(min < 0.1);
		assertTrue(delta / (map.width * map.height) < 0.01);
	}
	@SneakyThrows private static byte[] load(String name) {
		try (InputStream input = FingerprintTemplateTest.class.getResourceAsStream("/com/machinezoo/sourceafis/" + name)) {
			return IOUtils.toByteArray(input);
		}
	}
}
