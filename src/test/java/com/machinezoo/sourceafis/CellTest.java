// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.*;

public class CellTest {
	@Test public void constructor() {
		Cell c = new Cell(2, 3);
		assertEquals(2, c.x);
		assertEquals(3, c.y);
	}
	@Test public void area() {
		assertEquals(6, new Cell(2, 3).area());
	}
	@Test public void lengthSq() {
		assertEquals(5 * 5, new Cell(3, 4).lengthSq());
		assertEquals(5 * 5, new Cell(-3, -4).lengthSq());
	}
	@Test public void contains() {
		Cell c = new Cell(3, 4);
		assertTrue(c.contains(new Cell(1, 1)));
		assertTrue(c.contains(new Cell(0, 0)));
		assertTrue(c.contains(new Cell(2, 3)));
		assertTrue(c.contains(new Cell(0, 3)));
		assertTrue(c.contains(new Cell(2, 0)));
		assertFalse(c.contains(new Cell(-1, 1)));
		assertFalse(c.contains(new Cell(1, -1)));
		assertFalse(c.contains(new Cell(-2, -3)));
		assertFalse(c.contains(new Cell(1, 4)));
		assertFalse(c.contains(new Cell(3, 1)));
		assertFalse(c.contains(new Cell(1, 7)));
		assertFalse(c.contains(new Cell(5, 1)));
		assertFalse(c.contains(new Cell(8, 9)));
	}
	@Test public void plus() {
		assertEquals(new Cell(6, 8), new Cell(2, 3).plus(new Cell(4, 5)));
	}
	@Test public void minus() {
		assertEquals(new Cell(2, 3), new Cell(6, 8).minus(new Cell(4, 5)));
	}
	@Test public void negate() {
		assertEquals(new Cell(-2, -3), new Cell(2, 3).negate());
	}
	@Test public void toPoint() {
		PointTest.assertPointEquals(new Point(2, 3), new Cell(2, 3).toPoint(), 0.001);
	}
	@Test public void equals() {
		assertTrue(new Cell(2, 3).equals(new Cell(2, 3)));
		assertFalse(new Cell(2, 3).equals(new Cell(0, 3)));
		assertFalse(new Cell(2, 3).equals(new Cell(2, 0)));
		assertFalse(new Cell(2, 3).equals(null));
		assertFalse(new Cell(2, 3).equals(new Integer(1)));
	}
	@Test public void hashCodeTest() {
		assertEquals(new Cell(2, 3).hashCode(), new Cell(2, 3).hashCode());
		assertNotEquals(new Cell(2, 3).hashCode(), new Cell(-2, 3).hashCode());
		assertNotEquals(new Cell(2, 3).hashCode(), new Cell(2, -3).hashCode());
	}
	@Test public void edgeNeighbors() {
		Set<Cell> s = new HashSet<>();
		for (Cell n : Cell.edgeNeighbors) {
			s.add(n);
			assertEquals(1, n.lengthSq());
		}
		assertEquals(4, s.size());
	}
	@Test public void cornerNeighbors() {
		Set<Cell> s = new HashSet<>();
		for (Cell n : Cell.cornerNeighbors) {
			s.add(n);
			assertTrue(n.lengthSq() == 1 || n.lengthSq() == 2);
		}
		assertEquals(8, s.size());
	}
	@Test public void iterator() {
		List<Cell> l = new ArrayList<>();
		for (Cell c : new Cell(2, 3))
			l.add(c);
		assertEquals(Arrays.asList(new Cell(0, 0), new Cell(1, 0), new Cell(0, 1), new Cell(1, 1), new Cell(0, 2), new Cell(1, 2)), l);
		for (Cell c : new Cell(0, 3))
			fail(c.toString());
		for (Cell c : new Cell(3, 0))
			fail(c.toString());
		for (Cell c : new Cell(-1, 3))
			fail(c.toString());
		for (Cell c : new Cell(3, -1))
			fail(c.toString());
	}
	@Test public void lineTo() {
		checkLineTo(2, 3, 2, 3, 2, 3);
		checkLineTo(2, 3, 1, 4, 2, 3, 1, 4);
		checkLineTo(2, 3, -1, 3, 2, 3, 1, 3, 0, 3, -1, 3);
		checkLineTo(-1, 2, 0, -1, -1, 2, -1, 1, 0, 0, 0, -1);
		checkLineTo(1, 1, 3, 7, 1, 1, 1, 2, 2, 3, 2, 4, 2, 5, 3, 6, 3, 7);
		checkLineTo(1, 3, 6, 1, 1, 3, 2, 3, 3, 2, 4, 2, 5, 1, 6, 1);
	}
	private void checkLineTo(int x1, int y1, int x2, int y2, int... p) {
		Cell[] l = new Cell[p.length / 2];
		for (int i = 0; i < l.length; ++i)
			l[i] = new Cell(p[2 * i], p[2 * i + 1]);
		assertArrayEquals(l, new Cell(x1, y1).lineTo(new Cell(x2, y2)));
	}
	@Test public void toString_readable() {
		assertEquals("[2,3]", new Cell(2, 3).toString());
	}
}
