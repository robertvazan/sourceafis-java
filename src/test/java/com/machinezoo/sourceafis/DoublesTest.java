// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import org.junit.*;

public class DoublesTest {
	@Test public void sq() {
		assertEquals(6.25, Doubles.sq(2.5), 0.001);
		assertEquals(6.25, Doubles.sq(-2.5), 0.001);
	}
	@Test public void interpolate1D() {
		assertEquals(5, Doubles.interpolate(3, 7, 0.5), 0.001);
		assertEquals(3, Doubles.interpolate(3, 7, 0), 0.001);
		assertEquals(7, Doubles.interpolate(3, 7, 1), 0.001);
		assertEquals(6, Doubles.interpolate(7, 3, 0.25), 0.001);
		assertEquals(11, Doubles.interpolate(7, 3, -1), 0.001);
		assertEquals(9, Doubles.interpolate(3, 7, 1.5), 0.001);
	}
	@Test public void interpolate2D() {
		assertEquals(2, Doubles.interpolate(3, 7, 2, 4, 0, 0), 0.001);
		assertEquals(4, Doubles.interpolate(3, 7, 2, 4, 1, 0), 0.001);
		assertEquals(3, Doubles.interpolate(3, 7, 2, 4, 0, 1), 0.001);
		assertEquals(7, Doubles.interpolate(3, 7, 2, 4, 1, 1), 0.001);
		assertEquals(2.5, Doubles.interpolate(3, 7, 2, 4, 0, 0.5), 0.001);
		assertEquals(5.5, Doubles.interpolate(3, 7, 2, 4, 1, 0.5), 0.001);
		assertEquals(3, Doubles.interpolate(3, 7, 2, 4, 0.5, 0), 0.001);
		assertEquals(5, Doubles.interpolate(3, 7, 2, 4, 0.5, 1), 0.001);
		assertEquals(4, Doubles.interpolate(3, 7, 2, 4, 0.5, 0.5), 0.001);
	}
	@Test public void interpolateExponential() {
		assertEquals(3, Doubles.interpolateExponential(3, 10, 0), 0.001);
		assertEquals(10, Doubles.interpolateExponential(3, 10, 1), 0.001);
		assertEquals(3, Doubles.interpolateExponential(1, 9, 0.5), 0.001);
		assertEquals(27, Doubles.interpolateExponential(1, 9, 1.5), 0.001);
		assertEquals(1 / 3.0, Doubles.interpolateExponential(1, 9, -0.5), 0.001);
	}
}
