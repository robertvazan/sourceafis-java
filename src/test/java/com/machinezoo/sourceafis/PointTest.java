// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import org.junit.*;

public class PointTest {
	@Test public void constructor() {
		Point p = new Point(2.5, 3.5);
		assertEquals(2.5, p.x, 0.001);
		assertEquals(3.5, p.y, 0.001);
	}
	@Test public void add() {
		assertPointEquals(new Point(6, 8), new Point(2, 3).add(new Point(4, 5)), 0.001);
	}
	@Test public void multiply() {
		assertPointEquals(new Point(1, 1.5), new Point(2, 3).multiply(0.5), 0.001);
	}
	@Test public void round() {
		assertEquals(new Cell(2, 3), new Point(2.4, 2.6).round());
		assertEquals(new Cell(-2, -3), new Point(-2.4, -2.6).round());
	}
	public static void assertPointEquals(Point expected, Point actual, double tolerance) {
		assertEquals(expected.x, actual.x, tolerance);
	}
}
