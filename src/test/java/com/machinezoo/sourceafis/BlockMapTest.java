// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import org.junit.*;

public class BlockMapTest {
	@Test public void constructor() {
		BlockMap m = new BlockMap(400, 600, 20);
		assertEquals(new IntPoint(400, 600), m.pixels);
		assertEquals(new IntPoint(20, 30), m.primary.blocks);
		assertEquals(new IntPoint(21, 31), m.primary.corners);
		assertEquals(new IntPoint(21, 31), m.secondary.blocks);
		assertEquals(new IntPoint(22, 32), m.secondary.corners);
		assertEquals(new IntPoint(0, 0), m.primary.corner(0, 0));
		assertEquals(new IntPoint(400, 600), m.primary.corner(20, 30));
		assertEquals(new IntPoint(200, 300), m.primary.corner(10, 15));
		assertEquals(new IntRect(0, 0, 20, 20), m.primary.block(0, 0));
		assertEquals(new IntRect(380, 580, 20, 20), m.primary.block(19, 29));
		assertEquals(new IntRect(200, 300, 20, 20), m.primary.block(10, 15));
		assertEquals(new IntRect(0, 0, 10, 10), m.secondary.block(0, 0));
		assertEquals(new IntRect(390, 590, 10, 10), m.secondary.block(20, 30));
		assertEquals(new IntRect(190, 290, 20, 20), m.secondary.block(10, 15));
	}
}
