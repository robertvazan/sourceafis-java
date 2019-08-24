// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import org.junit.*;

public class DoubleMapTest {
	private final DoubleMap m = new DoubleMap(3, 4);
	public DoubleMapTest() {
		for (int x = 0; x < m.width; ++x)
			for (int y = 0; y < m.height; ++y)
				m.set(x, y, 10 * x + y);
	}
	@Test public void constructor() {
		assertEquals(3, m.width);
		assertEquals(4, m.height);
	}
	@Test public void constructorFromCell() {
		DoubleMap m = new DoubleMap(new IntPoint(3, 4));
		assertEquals(3, m.width);
		assertEquals(4, m.height);
	}
	@Test public void size() {
		assertEquals(3, m.size().x);
		assertEquals(4, m.size().y);
	}
	@Test public void getAt() {
		assertEquals(12, m.get(1, 2), 0.001);
		assertEquals(21, m.get(2, 1), 0.001);
	}
	@Test public void getCell() {
		assertEquals(3, m.get(new IntPoint(0, 3)), 0.001);
		assertEquals(22, m.get(new IntPoint(2, 2)), 0.001);
	}
	@Test public void setAt() {
		m.set(1, 2, 101);
		assertEquals(101, m.get(1, 2), 0.001);
	}
	@Test public void setCell() {
		m.set(new IntPoint(2, 3), 101);
		assertEquals(101, m.get(2, 3), 0.001);
	}
	@Test public void addAt() {
		m.add(2, 1, 100);
		assertEquals(121, m.get(2, 1), 0.001);
	}
	@Test public void addCell() {
		m.add(new IntPoint(2, 3), 100);
		assertEquals(123, m.get(2, 3), 0.001);
	}
	@Test public void multiplyAt() {
		m.multiply(1, 3, 10);
		assertEquals(130, m.get(1, 3), 0.001);
	}
	@Test public void multiplyCell() {
		m.multiply(new IntPoint(1, 2), 10);
		assertEquals(120, m.get(1, 2), 0.001);
	}
}
