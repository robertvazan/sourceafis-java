// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

import static org.junit.Assert.*;
import org.junit.*;

public class CellGridTest {
	private final CellGrid g = new CellGrid(2, 3);
	public CellGridTest() {
		for (int i = 0; i < g.allX.length; ++i)
			g.allX[i] = (i + 1) * 10;
		for (int i = 0; i < g.allY.length; ++i)
			g.allY[i] = (i + 1) * 100;
	}
	@Test public void constructor() {
		assertEquals(2, g.allX.length);
		assertEquals(3, g.allY.length);
	}
	@Test public void constructorFromCell() {
		CellGrid g = new CellGrid(new Cell(2, 3));
		assertEquals(2, g.allX.length);
		assertEquals(3, g.allY.length);
	}
	@Test public void getAt() {
		assertEquals(new Cell(20, 300), g.get(1, 2));
	}
	@Test public void getCell() {
		assertEquals(new Cell(10, 200), g.get(new Cell(0, 1)));
	}
}
