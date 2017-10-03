package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import java.io.*;
import org.apache.commons.io.*;
import org.junit.*;
import com.machinezoo.sourceafis.models.*;
import lombok.*;

public class FingerprintTemplateTest {
	private static FingerprintTemplate t = new FingerprintTemplate("[]");
	public static FingerprintTemplate probe() {
		return new FingerprintTemplate(load("probe.png"));
	}
	public static FingerprintTemplate matching() {
		return new FingerprintTemplate(load("matching.png"));
	}
	public static FingerprintTemplate nonmatching() {
		return new FingerprintTemplate(load("nonmatching.png"));
	}
	public static FingerprintTemplate probeIso() {
		return new FingerprintTemplate(load("iso-probe.dat"));
	}
	public static FingerprintTemplate matchingIso() {
		return new FingerprintTemplate(load("iso-matching.dat"));
	}
	public static FingerprintTemplate nonmatchingIso() {
		return new FingerprintTemplate(load("iso-nonmatching.dat"));
	}
	@Test public void constructor() {
		new FingerprintTemplate(load("probe.png"));
	}
	@Test public void readImage_png() {
		readImage_validate(t.readImage(load("probe.png")));
	}
	@Test public void readImage_jpeg() {
		readImage_validate(t.readImage(load("probe.jpeg")));
	}
	@Test public void readImage_bmp() {
		readImage_validate(t.readImage(load("probe.bmp")));
	}
	private void readImage_validate(DoubleMap map) {
		assertEquals(388, map.width);
		assertEquals(374, map.height);
		DoubleMap reference = t.readImage(load("probe.png"));
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
	@Test public void json_roundTrip() {
		t.minutiae = new FingerprintMinutia[] {
			new FingerprintMinutia(new Cell(100, 200), Math.PI, MinutiaType.BIFURCATION),
			new FingerprintMinutia(new Cell(300, 400), 0.5 * Math.PI, MinutiaType.ENDING)
		};
		t = new FingerprintTemplate(t.json());
		assertEquals(2, t.minutiae.length);
		FingerprintMinutia a = t.minutiae[0];
		FingerprintMinutia b = t.minutiae[1];
		assertEquals(new Cell(100, 200), a.position);
		assertEquals(Math.PI, a.direction, 0.0000001);
		assertEquals(MinutiaType.BIFURCATION, a.type);
		assertEquals(new Cell(300, 400), b.position);
		assertEquals(0.5 * Math.PI, b.direction, 0.0000001);
		assertEquals(MinutiaType.ENDING, b.type);
	}
	@SneakyThrows private static byte[] load(String name) {
		try (InputStream input = FingerprintTemplateTest.class.getResourceAsStream("/com/machinezoo/sourceafis/" + name)) {
			return IOUtils.toByteArray(input);
		}
	}
}
