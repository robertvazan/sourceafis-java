// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.*;

public class BlockTest {
	@Test public void constructor() {
		Block b = new Block(2, 3, 10, 20);
		assertEquals(2, b.x);
		assertEquals(3, b.y);
		assertEquals(10, b.width);
		assertEquals(20, b.height);
	}
	@Test public void constructorFromCell() {
		Block b = new Block(new Cell(2, 3));
		assertEquals(0, b.x);
		assertEquals(0, b.y);
		assertEquals(2, b.width);
		assertEquals(3, b.height);
	}
	@Test public void left() {
		assertEquals(2, new Block(2, 3, 4, 5).left());
	}
	@Test public void right() {
		assertEquals(6, new Block(2, 3, 4, 5).right());
	}
	@Test public void bottom() {
		assertEquals(3, new Block(2, 3, 4, 5).top());
	}
	@Test public void top() {
		assertEquals(8, new Block(2, 3, 4, 5).bottom());
	}
	@Test public void area() {
		assertEquals(20, new Block(2, 3, 4, 5).area());
	}
	@Test public void betweenCoordinates() {
		assertEquals(new Block(2, 3, 4, 5), Block.between(2, 3, 6, 8));
	}
	@Test public void betweenCells() {
		assertEquals(new Block(2, 3, 4, 5), Block.between(new Cell(2, 3), new Cell(6, 8)));
	}
	@Test public void aroundCoordinates() {
		assertEquals(new Block(2, 3, 5, 5), Block.around(4, 5, 2));
	}
	@Test public void aroundCell() {
		assertEquals(new Block(2, 3, 5, 5), Block.around(new Cell(4, 5), 2));
	}
	@Test public void center() {
		assertEquals(new Cell(4, 5), new Block(2, 3, 4, 4).center());
		assertEquals(new Cell(4, 5), new Block(2, 3, 5, 5).center());
		assertEquals(new Cell(2, 3), new Block(2, 3, 0, 0).center());
	}
	@Test public void move() {
		assertEquals(new Block(12, 23, 4, 5), new Block(2, 3, 4, 5).move(new Cell(10, 20)));
	}
	@Test public void intersect() {
		assertEquals(new Block(58, 30, 2, 5), new Block(20, 30, 40, 50).intersect(new Block(58, 27, 7, 8)));
		assertEquals(new Block(20, 77, 5, 3), new Block(20, 30, 40, 50).intersect(new Block(18, 77, 7, 8)));
		assertEquals(new Block(30, 40, 20, 30), new Block(20, 30, 40, 50).intersect(new Block(30, 40, 20, 30)));
	}
	@Test public void iterator() {
		List<Cell> l = new ArrayList<>();
		for (Cell c : new Block(4, 5, 2, 3))
			l.add(c);
		assertEquals(Arrays.asList(new Cell(4, 5), new Cell(5, 5), new Cell(4, 6), new Cell(5, 6), new Cell(4, 7), new Cell(5, 7)), l);
		for (Cell c : new Block(2, 3, 0, 3))
			fail(c.toString());
		for (Cell c : new Block(2, 3, 3, 0))
			fail(c.toString());
		for (Cell c : new Block(2, 3, -1, 3))
			fail(c.toString());
		for (Cell c : new Block(2, 3, 3, -1))
			fail(c.toString());
	}
	@Test public void toString_readable() {
		assertEquals("[10,20] @ [2,3]", new Block(2, 3, 10, 20).toString());
	}
}
