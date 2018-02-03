// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import org.junit.*;

public class AngleTest {
	@Test public void toVector() {
		PointTest.assertPointEquals(new Point(1, 0), Angle.toVector(-Angle.PI2), 0.01);
		PointTest.assertPointEquals(new Point(0, 1), Angle.toVector(-1.5 * Math.PI), 0.01);
		PointTest.assertPointEquals(new Point(-1, 0), Angle.toVector(-Math.PI), 0.01);
		PointTest.assertPointEquals(new Point(0, -1), Angle.toVector(-0.5 * Math.PI), 0.01);
		PointTest.assertPointEquals(new Point(1, 0), Angle.toVector(0), 0.01);
		PointTest.assertPointEquals(new Point(Math.sqrt(2) / 2, Math.sqrt(2) / 2), Angle.toVector(Math.PI / 4), 0.01);
		PointTest.assertPointEquals(new Point(0, 1), Angle.toVector(Math.PI / 2), 0.01);
		PointTest.assertPointEquals(new Point(-1, 0), Angle.toVector(Math.PI), 0.01);
		PointTest.assertPointEquals(new Point(0, -1), Angle.toVector(1.5 * Math.PI), 0.01);
		PointTest.assertPointEquals(new Point(1, 0), Angle.toVector(Angle.PI2), 0.01);
		PointTest.assertPointEquals(new Point(0, 1), Angle.toVector(2.5 * Math.PI), 0.01);
		PointTest.assertPointEquals(new Point(-1, 0), Angle.toVector(3 * Math.PI), 0.01);
		PointTest.assertPointEquals(new Point(0, -1), Angle.toVector(3.5 * Math.PI), 0.01);
		PointTest.assertPointEquals(new Point(1, 0), Angle.toVector(2 * Angle.PI2), 0.01);
	}
	@Test public void atanPoint() {
		assertEquals(0, Angle.atan(new Point(5, 0)), 0.001);
		assertEquals(0.25 * Math.PI, Angle.atan(new Point(1, 1)), 0.001);
		assertEquals(0.5 * Math.PI, Angle.atan(new Point(0, 3)), 0.001);
		assertEquals(Math.PI, Angle.atan(new Point(-0.3, 0)), 0.001);
		assertEquals(1.5 * Math.PI, Angle.atan(new Point(0, -1)), 0.001);
		assertEquals(1.75 * Math.PI, Angle.atan(new Point(1, -1)), 0.001);
	}
	@Test public void atanCell() {
		assertEquals(0.5 * Math.PI, Angle.atan(new Cell(0, 2)), 0.001);
	}
	@Test public void atanCenter() {
		assertEquals(0.25 * Math.PI, Angle.atan(new Cell(2, 3), new Cell(4, 5)), 0.001);
	}
	@Test public void toOrientation() {
		assertEquals(0, Angle.toOrientation(0), 0.001);
		assertEquals(0.5 * Math.PI, Angle.toOrientation(0.25 * Math.PI), 0.001);
		assertEquals(Math.PI, Angle.toOrientation(0.5 * Math.PI), 0.001);
		assertEquals(2 * Math.PI, Angle.toOrientation(Math.PI - 0.000001), 0.001);
		assertEquals(0, Angle.toOrientation(Math.PI + 0.000001), 0.001);
		assertEquals(Math.PI, Angle.toOrientation(1.5 * Math.PI), 0.001);
		assertEquals(1.5 * Math.PI, Angle.toOrientation(1.75 * Math.PI), 0.001);
		assertEquals(2 * Math.PI, Angle.toOrientation(2 * Math.PI - 0.000001), 0.001);
	}
	@Test public void add() {
		assertEquals(0, Angle.add(0, 0), 0.001);
		assertEquals(0.75 * Math.PI, Angle.add(0.25 * Math.PI, 0.5 * Math.PI), 0.001);
		assertEquals(1.75 * Math.PI, Angle.add(Math.PI, 0.75 * Math.PI), 0.001);
		assertEquals(0.25 * Math.PI, Angle.add(Math.PI, 1.25 * Math.PI), 0.001);
		assertEquals(1.5 * Math.PI, Angle.add(1.75 * Math.PI, 1.75 * Math.PI), 0.001);
	}
	@Test public void opposite() {
		assertEquals(Math.PI, Angle.opposite(0), 0.001);
		assertEquals(1.25 * Math.PI, Angle.opposite(0.25 * Math.PI), 0.001);
		assertEquals(1.5 * Math.PI, Angle.opposite(0.5 * Math.PI), 0.001);
		assertEquals(2 * Math.PI, Angle.opposite(Math.PI - 0.000001), 0.001);
		assertEquals(0, Angle.opposite(Math.PI + 0.000001), 0.001);
		assertEquals(0.5 * Math.PI, Angle.opposite(1.5 * Math.PI), 0.001);
		assertEquals(Math.PI, Angle.opposite(2 * Math.PI - 0.000001), 0.001);
	}
	@Test public void distance() {
		assertEquals(Math.PI, Angle.distance(0, Math.PI), 0.001);
		assertEquals(Math.PI, Angle.distance(1.5 * Math.PI, 0.5 * Math.PI), 0.001);
		assertEquals(0.75 * Math.PI, Angle.distance(0.75 * Math.PI, 1.5 * Math.PI), 0.001);
		assertEquals(0.5 * Math.PI, Angle.distance(0.25 * Math.PI, 1.75 * Math.PI), 0.001);
	}
	@Test public void difference() {
		assertEquals(Math.PI, Angle.difference(0, Math.PI), 0.001);
		assertEquals(Math.PI, Angle.difference(1.5 * Math.PI, 0.5 * Math.PI), 0.001);
		assertEquals(1.25 * Math.PI, Angle.difference(0.75 * Math.PI, 1.5 * Math.PI), 0.001);
		assertEquals(0.5 * Math.PI, Angle.difference(0.25 * Math.PI, 1.75 * Math.PI), 0.001);
	}
	@Test public void complementary() {
		assertEquals(0, Angle.complementary(0), 0.001);
		assertEquals(1.5 * Math.PI, Angle.complementary(0.5 * Math.PI), 0.001);
		assertEquals(Math.PI, Angle.complementary(Math.PI - 0.0000001), 0.001);
		assertEquals(Math.PI, Angle.complementary(Math.PI + 0.0000001), 0.001);
		assertEquals(0.5 * Math.PI, Angle.complementary(1.5 * Math.PI), 0.001);
		assertEquals(0, Angle.complementary(2 * Math.PI - 0.0000001), 0.001);
	}
	@Test public void bucketCenter() {
		assertEquals(0.25 * Math.PI, Angle.bucketCenter(0, 4), 0.001);
		assertEquals(0.75 * Math.PI, Angle.bucketCenter(1, 4), 0.001);
		assertEquals(1.25 * Math.PI, Angle.bucketCenter(2, 4), 0.001);
		assertEquals(1.75 * Math.PI, Angle.bucketCenter(3, 4), 0.001);
	}
	@Test public void quantize() {
		assertEquals(0, Angle.quantize(-0.0001, 4));
		assertEquals(0, Angle.quantize(0, 4));
		assertEquals(0, Angle.quantize(0.25 * Math.PI, 4));
		assertEquals(2, Angle.quantize(Math.PI, 5));
		assertEquals(6, Angle.quantize(1.75 * Math.PI, 7));
		assertEquals(9, Angle.quantize(Angle.PI2 - 0.001, 10));
		assertEquals(9, Angle.quantize(Angle.PI2 + 0.001, 10));
	}
}
