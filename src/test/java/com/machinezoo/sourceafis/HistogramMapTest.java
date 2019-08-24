// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import org.junit.*;

public class HistogramMapTest {
	private final HistogramMap h = new HistogramMap(4, 5, 6);
	public HistogramMapTest() {
		for (int x = 0; x < h.width; ++x)
			for (int y = 0; y < h.height; ++y)
				for (int z = 0; z < h.depth; ++z)
					h.set(x, y, z, 100 * x + 10 * y + z);
	}
	@Test public void constructor() {
		assertEquals(4, h.width);
		assertEquals(5, h.height);
		assertEquals(6, h.depth);
	}
	@Test public void constrain() {
		assertEquals(3, h.constrain(3));
		assertEquals(0, h.constrain(0));
		assertEquals(5, h.constrain(5));
		assertEquals(0, h.constrain(-1));
		assertEquals(5, h.constrain(6));
	}
	@Test public void getAt() {
		assertEquals(234, h.get(2, 3, 4));
		assertEquals(312, h.get(3, 1, 2));
	}
	@Test public void getCell() {
		assertEquals(125, h.get(new IntPoint(1, 2), 5));
		assertEquals(243, h.get(new IntPoint(2, 4), 3));
	}
	@Test public void sumAt() {
		assertEquals(6 * 120 + 1 + 2 + 3 + 4 + 5, h.sum(1, 2));
	}
	@Test public void sumCell() {
		assertEquals(6 * 340 + 1 + 2 + 3 + 4 + 5, h.sum(new IntPoint(3, 4)));
	}
	@Test public void setAt() {
		h.set(2, 4, 3, 1000);
		assertEquals(1000, h.get(2, 4, 3));
	}
	@Test public void setCell() {
		h.set(new IntPoint(3, 1), 5, 1000);
		assertEquals(1000, h.get(3, 1, 5));
	}
	@Test public void addAt() {
		h.add(1, 2, 4, 1000);
		assertEquals(1124, h.get(1, 2, 4));
	}
	@Test public void addCell() {
		h.add(new IntPoint(2, 4), 1, 1000);
		assertEquals(1241, h.get(2, 4, 1));
	}
	@Test public void incrementAt() {
		h.increment(3, 4, 1);
		assertEquals(342, h.get(3, 4, 1));
	}
	@Test public void incrementCell() {
		h.increment(new IntPoint(2, 3), 5);
		assertEquals(236, h.get(2, 3, 5));
	}
}
