// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

import static org.junit.Assert.*;
import org.junit.*;

public class BlockGridTest {
	private final CellGrid cg = new CellGrid(3, 4);
	private final BlockGrid g = new BlockGrid(cg);
	public BlockGridTest() {
		for (int i = 0; i < cg.allX.length; ++i)
			cg.allX[i] = (i + 1) * 10;
		for (int i = 0; i < cg.allY.length; ++i)
			cg.allY[i] = (i + 1) * 100;
	}
	@Test public void getAt() {
		assertEquals(new Block(20, 300, 10, 100), g.get(1, 2));
	}
	@Test public void getCell() {
		assertEquals(new Block(10, 200, 10, 100), g.get(0, 1));
	}
}
