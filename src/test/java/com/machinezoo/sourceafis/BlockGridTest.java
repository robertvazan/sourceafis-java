// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

public class BlockGridTest {
	private final BlockGrid g = new BlockGrid(3, 4);
	public BlockGridTest() {
		for (int i = 0; i < g.x.length; ++i)
			g.x[i] = (i + 1) * 10;
		for (int i = 0; i < g.y.length; ++i)
			g.y[i] = (i + 1) * 100;
	}
	@Test
	public void constructor() {
		assertEquals(4, g.x.length);
		assertEquals(5, g.y.length);
	}
	@Test
	public void constructorFromPoint() {
		BlockGrid g = new BlockGrid(new IntPoint(2, 3));
		assertEquals(3, g.x.length);
		assertEquals(4, g.y.length);
	}
	@Test
	public void cornerXY() {
		assertEquals(new IntPoint(20, 300), g.corner(1, 2));
	}
	@Test
	public void cornerAt() {
		assertEquals(new IntPoint(10, 200), g.corner(new IntPoint(0, 1)));
	}
	@Test
	public void blockXY() {
		assertEquals(new IntRect(20, 300, 10, 100), g.block(1, 2));
	}
	@Test
	public void blockAt() {
		assertEquals(new IntRect(10, 200, 10, 100), g.block(new IntPoint(0, 1)));
	}
}
