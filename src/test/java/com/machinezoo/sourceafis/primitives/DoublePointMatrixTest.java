// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.primitives;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

public class DoublePointMatrixTest {
	private final DoublePointMatrix m = new DoublePointMatrix(4, 5);
	public DoublePointMatrixTest() {
		for (int x = 0; x < m.width; ++x)
			for (int y = 0; y < m.height; ++y)
				m.set(x, y, new DoublePoint(10 * x, 10 * y));
	}
	@Test
	public void constructor() {
		assertEquals(4, m.width);
		assertEquals(5, m.height);
	}
	@Test
	public void constructorFromPoint() {
		DoublePointMatrix m = new DoublePointMatrix(new IntPoint(4, 5));
		assertEquals(4, m.width);
		assertEquals(5, m.height);
	}
	@Test
	public void get() {
		DoublePointTest.assertPointEquals(new DoublePoint(20, 30), m.get(2, 3), 0.001);
		DoublePointTest.assertPointEquals(new DoublePoint(30, 10), m.get(3, 1), 0.001);
	}
	@Test
	public void getAt() {
		DoublePointTest.assertPointEquals(new DoublePoint(10, 20), m.get(new IntPoint(1, 2)), 0.001);
		DoublePointTest.assertPointEquals(new DoublePoint(20, 40), m.get(new IntPoint(2, 4)), 0.001);
	}
	@Test
	public void setValues() {
		m.set(2, 4, 101, 102);
		DoublePointTest.assertPointEquals(new DoublePoint(101, 102), m.get(2, 4), 0.001);
	}
	@Test
	public void set() {
		m.set(1, 2, new DoublePoint(101, 102));
		DoublePointTest.assertPointEquals(new DoublePoint(101, 102), m.get(1, 2), 0.001);
	}
	@Test
	public void setAt() {
		m.set(new IntPoint(3, 2), new DoublePoint(101, 102));
		DoublePointTest.assertPointEquals(new DoublePoint(101, 102), m.get(3, 2), 0.001);
	}
	@Test
	public void addValues() {
		m.add(3, 1, 100, 200);
		DoublePointTest.assertPointEquals(new DoublePoint(130, 210), m.get(3, 1), 0.001);
	}
	@Test
	public void add() {
		m.add(2, 3, new DoublePoint(100, 200));
		DoublePointTest.assertPointEquals(new DoublePoint(120, 230), m.get(2, 3), 0.001);
	}
	@Test
	public void addAt() {
		m.add(new IntPoint(2, 4), new DoublePoint(100, 200));
		DoublePointTest.assertPointEquals(new DoublePoint(120, 240), m.get(2, 4), 0.001);
	}
}
