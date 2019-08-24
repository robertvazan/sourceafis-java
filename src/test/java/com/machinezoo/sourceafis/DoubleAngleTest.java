// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import org.junit.*;

public class DoubleAngleTest {
	@Test public void toVector() {
		DoublePointTest.assertPointEquals(new DoublePoint(1, 0), DoubleAngle.toVector(-DoubleAngle.PI2), 0.01);
		DoublePointTest.assertPointEquals(new DoublePoint(0, 1), DoubleAngle.toVector(-1.5 * Math.PI), 0.01);
		DoublePointTest.assertPointEquals(new DoublePoint(-1, 0), DoubleAngle.toVector(-Math.PI), 0.01);
		DoublePointTest.assertPointEquals(new DoublePoint(0, -1), DoubleAngle.toVector(-0.5 * Math.PI), 0.01);
		DoublePointTest.assertPointEquals(new DoublePoint(1, 0), DoubleAngle.toVector(0), 0.01);
		DoublePointTest.assertPointEquals(new DoublePoint(Math.sqrt(2) / 2, Math.sqrt(2) / 2), DoubleAngle.toVector(Math.PI / 4), 0.01);
		DoublePointTest.assertPointEquals(new DoublePoint(0, 1), DoubleAngle.toVector(Math.PI / 2), 0.01);
		DoublePointTest.assertPointEquals(new DoublePoint(-1, 0), DoubleAngle.toVector(Math.PI), 0.01);
		DoublePointTest.assertPointEquals(new DoublePoint(0, -1), DoubleAngle.toVector(1.5 * Math.PI), 0.01);
		DoublePointTest.assertPointEquals(new DoublePoint(1, 0), DoubleAngle.toVector(DoubleAngle.PI2), 0.01);
		DoublePointTest.assertPointEquals(new DoublePoint(0, 1), DoubleAngle.toVector(2.5 * Math.PI), 0.01);
		DoublePointTest.assertPointEquals(new DoublePoint(-1, 0), DoubleAngle.toVector(3 * Math.PI), 0.01);
		DoublePointTest.assertPointEquals(new DoublePoint(0, -1), DoubleAngle.toVector(3.5 * Math.PI), 0.01);
		DoublePointTest.assertPointEquals(new DoublePoint(1, 0), DoubleAngle.toVector(2 * DoubleAngle.PI2), 0.01);
	}
	@Test public void atanPoint() {
		assertEquals(0, DoubleAngle.atan(new DoublePoint(5, 0)), 0.001);
		assertEquals(0.25 * Math.PI, DoubleAngle.atan(new DoublePoint(1, 1)), 0.001);
		assertEquals(0.5 * Math.PI, DoubleAngle.atan(new DoublePoint(0, 3)), 0.001);
		assertEquals(Math.PI, DoubleAngle.atan(new DoublePoint(-0.3, 0)), 0.001);
		assertEquals(1.5 * Math.PI, DoubleAngle.atan(new DoublePoint(0, -1)), 0.001);
		assertEquals(1.75 * Math.PI, DoubleAngle.atan(new DoublePoint(1, -1)), 0.001);
	}
	@Test public void atanCell() {
		assertEquals(0.5 * Math.PI, DoubleAngle.atan(new IntPoint(0, 2)), 0.001);
	}
	@Test public void atanCenter() {
		assertEquals(0.25 * Math.PI, DoubleAngle.atan(new IntPoint(2, 3), new IntPoint(4, 5)), 0.001);
	}
	@Test public void toOrientation() {
		assertEquals(0, DoubleAngle.toOrientation(0), 0.001);
		assertEquals(0.5 * Math.PI, DoubleAngle.toOrientation(0.25 * Math.PI), 0.001);
		assertEquals(Math.PI, DoubleAngle.toOrientation(0.5 * Math.PI), 0.001);
		assertEquals(2 * Math.PI, DoubleAngle.toOrientation(Math.PI - 0.000001), 0.001);
		assertEquals(0, DoubleAngle.toOrientation(Math.PI + 0.000001), 0.001);
		assertEquals(Math.PI, DoubleAngle.toOrientation(1.5 * Math.PI), 0.001);
		assertEquals(1.5 * Math.PI, DoubleAngle.toOrientation(1.75 * Math.PI), 0.001);
		assertEquals(2 * Math.PI, DoubleAngle.toOrientation(2 * Math.PI - 0.000001), 0.001);
	}
	@Test public void add() {
		assertEquals(0, DoubleAngle.add(0, 0), 0.001);
		assertEquals(0.75 * Math.PI, DoubleAngle.add(0.25 * Math.PI, 0.5 * Math.PI), 0.001);
		assertEquals(1.75 * Math.PI, DoubleAngle.add(Math.PI, 0.75 * Math.PI), 0.001);
		assertEquals(0.25 * Math.PI, DoubleAngle.add(Math.PI, 1.25 * Math.PI), 0.001);
		assertEquals(1.5 * Math.PI, DoubleAngle.add(1.75 * Math.PI, 1.75 * Math.PI), 0.001);
	}
	@Test public void opposite() {
		assertEquals(Math.PI, DoubleAngle.opposite(0), 0.001);
		assertEquals(1.25 * Math.PI, DoubleAngle.opposite(0.25 * Math.PI), 0.001);
		assertEquals(1.5 * Math.PI, DoubleAngle.opposite(0.5 * Math.PI), 0.001);
		assertEquals(2 * Math.PI, DoubleAngle.opposite(Math.PI - 0.000001), 0.001);
		assertEquals(0, DoubleAngle.opposite(Math.PI + 0.000001), 0.001);
		assertEquals(0.5 * Math.PI, DoubleAngle.opposite(1.5 * Math.PI), 0.001);
		assertEquals(Math.PI, DoubleAngle.opposite(2 * Math.PI - 0.000001), 0.001);
	}
	@Test public void distance() {
		assertEquals(Math.PI, DoubleAngle.distance(0, Math.PI), 0.001);
		assertEquals(Math.PI, DoubleAngle.distance(1.5 * Math.PI, 0.5 * Math.PI), 0.001);
		assertEquals(0.75 * Math.PI, DoubleAngle.distance(0.75 * Math.PI, 1.5 * Math.PI), 0.001);
		assertEquals(0.5 * Math.PI, DoubleAngle.distance(0.25 * Math.PI, 1.75 * Math.PI), 0.001);
	}
	@Test public void difference() {
		assertEquals(Math.PI, DoubleAngle.difference(0, Math.PI), 0.001);
		assertEquals(Math.PI, DoubleAngle.difference(1.5 * Math.PI, 0.5 * Math.PI), 0.001);
		assertEquals(1.25 * Math.PI, DoubleAngle.difference(0.75 * Math.PI, 1.5 * Math.PI), 0.001);
		assertEquals(0.5 * Math.PI, DoubleAngle.difference(0.25 * Math.PI, 1.75 * Math.PI), 0.001);
	}
	@Test public void complementary() {
		assertEquals(0, DoubleAngle.complementary(0), 0.001);
		assertEquals(1.5 * Math.PI, DoubleAngle.complementary(0.5 * Math.PI), 0.001);
		assertEquals(Math.PI, DoubleAngle.complementary(Math.PI - 0.0000001), 0.001);
		assertEquals(Math.PI, DoubleAngle.complementary(Math.PI + 0.0000001), 0.001);
		assertEquals(0.5 * Math.PI, DoubleAngle.complementary(1.5 * Math.PI), 0.001);
		assertEquals(0, DoubleAngle.complementary(2 * Math.PI - 0.0000001), 0.001);
	}
	@Test public void bucketCenter() {
		assertEquals(0.25 * Math.PI, DoubleAngle.bucketCenter(0, 4), 0.001);
		assertEquals(0.75 * Math.PI, DoubleAngle.bucketCenter(1, 4), 0.001);
		assertEquals(1.25 * Math.PI, DoubleAngle.bucketCenter(2, 4), 0.001);
		assertEquals(1.75 * Math.PI, DoubleAngle.bucketCenter(3, 4), 0.001);
	}
	@Test public void quantize() {
		assertEquals(0, DoubleAngle.quantize(-0.0001, 4));
		assertEquals(0, DoubleAngle.quantize(0, 4));
		assertEquals(0, DoubleAngle.quantize(0.25 * Math.PI, 4));
		assertEquals(2, DoubleAngle.quantize(Math.PI, 5));
		assertEquals(6, DoubleAngle.quantize(1.75 * Math.PI, 7));
		assertEquals(9, DoubleAngle.quantize(DoubleAngle.PI2 - 0.001, 10));
		assertEquals(9, DoubleAngle.quantize(DoubleAngle.PI2 + 0.001, 10));
	}
}
