// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.*;

public class FingerprintImageTest {
	@Test public void decodePNG() {
		new FingerprintImage().decode(TestResources.png());
	}
	private void assertSimilar(DoubleMatrix map, DoubleMatrix reference) {
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
		assertSimilar(new FingerprintImage().decode(image).matrix, new FingerprintImage().decode(reference).matrix);
	}
	@Test public void decodeJPEG() {
		assertSimilar(TestResources.jpeg(), TestResources.png());
	}
	@Test public void decodeBMP() {
		assertSimilar(TestResources.bmp(), TestResources.png());
	}
	@Test public void decodeTIFF() {
		assertSimilar(TestResources.tiff(), TestResources.png());
	}
	@Test public void decodeWSQ() {
		assertSimilar(TestResources.originalWsq(), TestResources.convertedWsq());
	}
	public static FingerprintImage probe() {
		return new FingerprintImage().decode(TestResources.probe());
	}
	public static FingerprintImage matching() {
		return new FingerprintImage().decode(TestResources.matching());
	}
	public static FingerprintImage nonmatching() {
		return new FingerprintImage().decode(TestResources.nonmatching());
	}
	public static FingerprintImage probeGray() {
		return new FingerprintImage().grayscale(332, 533, TestResources.probeGray());
	}
	public static FingerprintImage matchingGray() {
		return new FingerprintImage().grayscale(320, 407, TestResources.matchingGray());
	}
	public static FingerprintImage nonmatchingGray() {
		return new FingerprintImage().grayscale(333, 435, TestResources.nonmatchingGray());
	}
	@Test public void decodeGray() {
		double score = new FingerprintMatcher()
			.index(new FingerprintTemplate(probeGray()))
			.match(new FingerprintTemplate(matchingGray()));
		assertThat(score, greaterThan(40.0));
	}
}
