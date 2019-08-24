// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.*;

public class IntRectTest {
	@Test public void constructor() {
		IntRect b = new IntRect(2, 3, 10, 20);
		assertEquals(2, b.x);
		assertEquals(3, b.y);
		assertEquals(10, b.width);
		assertEquals(20, b.height);
	}
	@Test public void constructorFromCell() {
		IntRect b = new IntRect(new IntPoint(2, 3));
		assertEquals(0, b.x);
		assertEquals(0, b.y);
		assertEquals(2, b.width);
		assertEquals(3, b.height);
	}
	@Test public void left() {
		assertEquals(2, new IntRect(2, 3, 4, 5).left());
	}
	@Test public void right() {
		assertEquals(6, new IntRect(2, 3, 4, 5).right());
	}
	@Test public void bottom() {
		assertEquals(3, new IntRect(2, 3, 4, 5).top());
	}
	@Test public void top() {
		assertEquals(8, new IntRect(2, 3, 4, 5).bottom());
	}
	@Test public void area() {
		assertEquals(20, new IntRect(2, 3, 4, 5).area());
	}
	@Test public void betweenCoordinates() {
		assertEquals(new IntRect(2, 3, 4, 5), IntRect.between(2, 3, 6, 8));
	}
	@Test public void betweenCells() {
		assertEquals(new IntRect(2, 3, 4, 5), IntRect.between(new IntPoint(2, 3), new IntPoint(6, 8)));
	}
	@Test public void aroundCoordinates() {
		assertEquals(new IntRect(2, 3, 5, 5), IntRect.around(4, 5, 2));
	}
	@Test public void aroundCell() {
		assertEquals(new IntRect(2, 3, 5, 5), IntRect.around(new IntPoint(4, 5), 2));
	}
	@Test public void center() {
		assertEquals(new IntPoint(4, 5), new IntRect(2, 3, 4, 4).center());
		assertEquals(new IntPoint(4, 5), new IntRect(2, 3, 5, 5).center());
		assertEquals(new IntPoint(2, 3), new IntRect(2, 3, 0, 0).center());
	}
	@Test public void move() {
		assertEquals(new IntRect(12, 23, 4, 5), new IntRect(2, 3, 4, 5).move(new IntPoint(10, 20)));
	}
	@Test public void intersect() {
		assertEquals(new IntRect(58, 30, 2, 5), new IntRect(20, 30, 40, 50).intersect(new IntRect(58, 27, 7, 8)));
		assertEquals(new IntRect(20, 77, 5, 3), new IntRect(20, 30, 40, 50).intersect(new IntRect(18, 77, 7, 8)));
		assertEquals(new IntRect(30, 40, 20, 30), new IntRect(20, 30, 40, 50).intersect(new IntRect(30, 40, 20, 30)));
	}
	@Test public void iterator() {
		List<IntPoint> l = new ArrayList<>();
		for (IntPoint c : new IntRect(4, 5, 2, 3))
			l.add(c);
		assertEquals(Arrays.asList(new IntPoint(4, 5), new IntPoint(5, 5), new IntPoint(4, 6), new IntPoint(5, 6), new IntPoint(4, 7), new IntPoint(5, 7)), l);
		for (IntPoint c : new IntRect(2, 3, 0, 3))
			fail(c.toString());
		for (IntPoint c : new IntRect(2, 3, 3, 0))
			fail(c.toString());
		for (IntPoint c : new IntRect(2, 3, -1, 3))
			fail(c.toString());
		for (IntPoint c : new IntRect(2, 3, 3, -1))
			fail(c.toString());
	}
	@Test public void toString_readable() {
		assertEquals("[10,20] @ [2,3]", new IntRect(2, 3, 10, 20).toString());
	}
}
