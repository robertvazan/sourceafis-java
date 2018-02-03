// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import org.junit.*;

public class PointMapTest {
	private final PointMap m = new PointMap(4, 5);
	public PointMapTest() {
		for (int x = 0; x < m.width; ++x)
			for (int y = 0; y < m.height; ++y)
				m.set(x, y, new Point(10 * x, 10 * y));
	}
	@Test public void constructor() {
		assertEquals(4, m.width);
		assertEquals(5, m.height);
	}
	@Test public void constructorFromCell() {
		PointMap m = new PointMap(new Cell(4, 5));
		assertEquals(4, m.width);
		assertEquals(5, m.height);
	}
	@Test public void getAt() {
		PointTest.assertPointEquals(new Point(20, 30), m.get(2, 3), 0.001);
		PointTest.assertPointEquals(new Point(30, 10), m.get(3, 1), 0.001);
	}
	@Test public void getCell() {
		PointTest.assertPointEquals(new Point(10, 20), m.get(new Cell(1, 2)), 0.001);
		PointTest.assertPointEquals(new Point(20, 40), m.get(new Cell(2, 4)), 0.001);
	}
	@Test public void setValues() {
		m.set(2, 4, 101, 102);
		PointTest.assertPointEquals(new Point(101, 102), m.get(2, 4), 0.001);
	}
	@Test public void setAt() {
		m.set(1, 2, new Point(101, 102));
		PointTest.assertPointEquals(new Point(101, 102), m.get(1, 2), 0.001);
	}
	@Test public void setCell() {
		m.set(new Cell(3, 2), new Point(101, 102));
		PointTest.assertPointEquals(new Point(101, 102), m.get(3, 2), 0.001);
	}
	@Test public void addValues() {
		m.add(3, 1, 100, 200);
		PointTest.assertPointEquals(new Point(130, 210), m.get(3, 1), 0.001);
	}
	@Test public void addAt() {
		m.add(2, 3, new Point(100, 200));
		PointTest.assertPointEquals(new Point(120, 230), m.get(2, 3), 0.001);
	}
	@Test public void addCell() {
		m.add(new Cell(2, 4), new Point(100, 200));
		PointTest.assertPointEquals(new Point(120, 240), m.get(2, 4), 0.001);
	}
}
