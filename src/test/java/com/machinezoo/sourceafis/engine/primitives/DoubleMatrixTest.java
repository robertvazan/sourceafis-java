// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.primitives;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

public class DoubleMatrixTest {
	private final DoubleMatrix m = new DoubleMatrix(3, 4);
	public DoubleMatrixTest() {
		for (int x = 0; x < m.width; ++x)
			for (int y = 0; y < m.height; ++y)
				m.set(x, y, 10 * x + y);
	}
	@Test
	public void constructor() {
		assertEquals(3, m.width);
		assertEquals(4, m.height);
	}
	@Test
	public void constructorFromPoint() {
		DoubleMatrix m = new DoubleMatrix(new IntPoint(3, 4));
		assertEquals(3, m.width);
		assertEquals(4, m.height);
	}
	@Test
	public void size() {
		assertEquals(3, m.size().x);
		assertEquals(4, m.size().y);
	}
	@Test
	public void get() {
		assertEquals(12, m.get(1, 2), 0.001);
		assertEquals(21, m.get(2, 1), 0.001);
	}
	@Test
	public void getAt() {
		assertEquals(3, m.get(new IntPoint(0, 3)), 0.001);
		assertEquals(22, m.get(new IntPoint(2, 2)), 0.001);
	}
	@Test
	public void set() {
		m.set(1, 2, 101);
		assertEquals(101, m.get(1, 2), 0.001);
	}
	@Test
	public void setAt() {
		m.set(new IntPoint(2, 3), 101);
		assertEquals(101, m.get(2, 3), 0.001);
	}
	@Test
	public void add() {
		m.add(2, 1, 100);
		assertEquals(121, m.get(2, 1), 0.001);
	}
	@Test
	public void addAt() {
		m.add(new IntPoint(2, 3), 100);
		assertEquals(123, m.get(2, 3), 0.001);
	}
	@Test
	public void multiply() {
		m.multiply(1, 3, 10);
		assertEquals(130, m.get(1, 3), 0.001);
	}
	@Test
	public void multiplyAt() {
		m.multiply(new IntPoint(1, 2), 10);
		assertEquals(120, m.get(1, 2), 0.001);
	}
}
