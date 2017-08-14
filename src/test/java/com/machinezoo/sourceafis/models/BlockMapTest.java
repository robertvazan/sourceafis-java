// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

import static org.junit.Assert.*;
import org.junit.*;

public class BlockMapTest {
	@Test public void constructor() {
		BlockMap m = new BlockMap(400, 600, 20);
		assertEquals(new Cell(400, 600), m.pixelCount);
		assertEquals(new Cell(20, 30), m.blockCount);
		assertEquals(new Cell(21, 31), m.cornerCount);
		assertEquals(new Cell(0, 0), m.corners.get(0, 0));
		assertEquals(new Cell(400, 600), m.corners.get(20, 30));
		assertEquals(new Cell(200, 300), m.corners.get(10, 15));
		assertEquals(new Block(0, 0, 20, 20), m.blockAreas.get(0, 0));
		assertEquals(new Block(380, 580, 20, 20), m.blockAreas.get(19, 29));
		assertEquals(new Block(200, 300, 20, 20), m.blockAreas.get(10, 15));
		assertEquals(new Cell(10, 10), m.blockCenters.get(0, 0));
		assertEquals(new Cell(390, 590), m.blockCenters.get(19, 29));
		assertEquals(new Cell(210, 310), m.blockCenters.get(10, 15));
		assertEquals(new Block(0, 0, 10, 10), m.cornerAreas.get(0, 0));
		assertEquals(new Block(390, 590, 10, 10), m.cornerAreas.get(20, 30));
		assertEquals(new Block(190, 290, 20, 20), m.cornerAreas.get(10, 15));
	}
}
