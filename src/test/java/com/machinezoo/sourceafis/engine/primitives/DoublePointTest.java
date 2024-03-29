// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.primitives;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

public class DoublePointTest {
	@Test
	public void constructor() {
		DoublePoint p = new DoublePoint(2.5, 3.5);
		assertEquals(2.5, p.x, 0.001);
		assertEquals(3.5, p.y, 0.001);
	}
	@Test
	public void add() {
		assertPointEquals(new DoublePoint(6, 8), new DoublePoint(2, 3).add(new DoublePoint(4, 5)), 0.001);
	}
	@Test
	public void multiply() {
		assertPointEquals(new DoublePoint(1, 1.5), new DoublePoint(2, 3).multiply(0.5), 0.001);
	}
	@Test
	public void round() {
		assertEquals(new IntPoint(2, 3), new DoublePoint(2.4, 2.6).round());
		assertEquals(new IntPoint(-2, -3), new DoublePoint(-2.4, -2.6).round());
	}
	static void assertPointEquals(DoublePoint expected, DoublePoint actual, double tolerance) {
		assertEquals(expected.x, actual.x, tolerance);
		assertEquals(expected.y, actual.y, tolerance);
	}
}
