// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.primitives;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

public class BooleanMatrixTest {
	private final BooleanMatrix m = new BooleanMatrix(4, 5);
	public BooleanMatrixTest() {
		for (int x = 0; x < m.width; ++x)
			for (int y = 0; y < m.height; ++y)
				m.set(x, y, (x + y) % 2 > 0);
	}
	@Test
	public void constructor() {
		assertEquals(4, m.width);
		assertEquals(5, m.height);
	}
	@Test
	public void constructorFromPoint() {
		BooleanMatrix m = new BooleanMatrix(new IntPoint(4, 5));
		assertEquals(4, m.width);
		assertEquals(5, m.height);
	}
	@Test
	public void constructorCloning() {
		BooleanMatrix m = new BooleanMatrix(this.m);
		assertEquals(4, m.width);
		assertEquals(5, m.height);
		for (int x = 0; x < m.width; ++x)
			for (int y = 0; y < m.height; ++y)
				assertEquals(this.m.get(x, y), m.get(x, y));
	}
	@Test
	public void size() {
		assertEquals(4, m.size().x);
		assertEquals(5, m.size().y);
	}
	@Test
	public void get() {
		assertEquals(true, m.get(1, 4));
		assertEquals(false, m.get(3, 1));
	}
	@Test
	public void getAt() {
		assertEquals(true, m.get(new IntPoint(3, 2)));
		assertEquals(false, m.get(new IntPoint(2, 4)));
	}
	@Test
	public void getFallback() {
		assertEquals(false, m.get(0, 0, true));
		assertEquals(true, m.get(3, 0, false));
		assertEquals(false, m.get(0, 4, true));
		assertEquals(true, m.get(3, 4, false));
		assertEquals(false, m.get(-1, 4, false));
		assertEquals(true, m.get(-1, 4, true));
		assertEquals(false, m.get(2, -1, false));
		assertEquals(true, m.get(4, 2, true));
		assertEquals(false, m.get(2, 5, false));
	}
	@Test
	public void getAtFallback() {
		assertEquals(false, m.get(new IntPoint(0, 0), true));
		assertEquals(true, m.get(new IntPoint(3, 0), false));
		assertEquals(false, m.get(new IntPoint(0, 4), true));
		assertEquals(true, m.get(new IntPoint(3, 4), false));
		assertEquals(false, m.get(new IntPoint(-1, 2), false));
		assertEquals(true, m.get(new IntPoint(-1, 2), true));
		assertEquals(false, m.get(new IntPoint(0, -1), false));
		assertEquals(true, m.get(new IntPoint(4, 0), true));
		assertEquals(false, m.get(new IntPoint(0, 5), false));
	}
	@Test
	public void set() {
		assertEquals(false, m.get(2, 4));
		m.set(2, 4, true);
		assertEquals(true, m.get(2, 4));
	}
	@Test
	public void setAt() {
		assertEquals(true, m.get(1, 2));
		m.set(new IntPoint(1, 2), false);
		assertEquals(false, m.get(1, 2));
	}
	@Test
	public void invert() {
		m.invert();
		assertEquals(true, m.get(0, 0));
		assertEquals(false, m.get(3, 0));
		assertEquals(true, m.get(0, 4));
		assertEquals(false, m.get(3, 4));
		assertEquals(true, m.get(1, 3));
		assertEquals(false, m.get(2, 1));
	}
	@Test
	public void merge() {
		assertEquals(true, m.get(3, 2));
		BooleanMatrix o = new BooleanMatrix(4, 5);
		for (int x = 0; x < m.width; ++x)
			for (int y = 0; y < m.height; ++y)
				o.set(x, y, x < 2 && y < 3);
		m.merge(o);
		assertEquals(true, m.get(0, 0));
		assertEquals(true, m.get(1, 2));
		assertEquals(false, m.get(1, 3));
		assertEquals(true, m.get(3, 2));
		for (int x = 0; x < m.width; ++x)
			for (int y = 0; y < m.height; ++y)
				assertEquals((x + y) % 2 > 0 || x < 2 && y < 3, m.get(x, y));
	}
}
