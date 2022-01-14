// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.machinezoo.sourceafis.engine.primitives.*;

public class FingerprintImageTest {
	@Test
	public void decodePNG() {
		new FingerprintImage(TestResources.png());
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
		assertSimilar(new FingerprintImage(image).matrix, new FingerprintImage(reference).matrix);
	}
	@Test
	public void decodeJPEG() {
		assertSimilar(TestResources.jpeg(), TestResources.png());
	}
	@Test
	public void decodeBMP() {
		assertSimilar(TestResources.bmp(), TestResources.png());
	}
	@Test
	public void decodeWSQ() {
		assertSimilar(TestResources.originalWsq(), TestResources.convertedWsq());
	}
	public static FingerprintImage probe() {
		return new FingerprintImage(TestResources.probe());
	}
	public static FingerprintImage matching() {
		return new FingerprintImage(TestResources.matching());
	}
	public static FingerprintImage nonmatching() {
		return new FingerprintImage(TestResources.nonmatching());
	}
	public static FingerprintImage probeGray() {
		return new FingerprintImage(332, 533, TestResources.probeGray());
	}
	public static FingerprintImage matchingGray() {
		return new FingerprintImage(320, 407, TestResources.matchingGray());
	}
	public static FingerprintImage nonmatchingGray() {
		return new FingerprintImage(333, 435, TestResources.nonmatchingGray());
	}
	@Test
	public void decodeGray() {
		double score = new FingerprintMatcher(new FingerprintTemplate(probeGray()))
			.match(new FingerprintTemplate(matchingGray()));
		assertThat(score, greaterThan(40.0));
	}
}
