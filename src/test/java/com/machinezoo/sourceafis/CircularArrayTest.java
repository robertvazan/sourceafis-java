// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import org.junit.*;

public class CircularArrayTest {
	private final CircularArray a = new CircularArray(16);
	public CircularArrayTest() {
		a.insert(0, 16);
		a.remove(0, 10);
		a.insert(a.size, 4);
		for (int i = 0; i < a.size; ++i)
			a.set(i, i + 1);
	}
	@Test public void constructor() {
		CircularArray a = new CircularArray(13);
		assertEquals(13, a.array.length);
		assertEquals(0, a.size);
		assertEquals(0, a.head);
	}
	@Test public void validateItemIndex() {
		a.validateItemIndex(0);
		a.validateItemIndex(5);
		a.validateItemIndex(9);
	}
	@Test(expected = IndexOutOfBoundsException.class) public void validateItemIndex_underflow() {
		a.validateItemIndex(-1);
	}
	@Test(expected = IndexOutOfBoundsException.class) public void validateItemIndex_overflow() {
		a.validateItemIndex(10);
	}
	@Test public void validateCursorIndex() {
		a.validateCursorIndex(0);
		a.validateCursorIndex(4);
		a.validateCursorIndex(10);
	}
	@Test(expected = IndexOutOfBoundsException.class) public void validateCursorIndex_underflow() {
		a.validateCursorIndex(-1);
	}
	@Test(expected = IndexOutOfBoundsException.class) public void validateCursorIndex_overflow() {
		a.validateCursorIndex(11);
	}
	@Test public void location() {
		assertEquals(10, a.location(0));
		assertEquals(14, a.location(4));
		assertEquals(15, a.location(5));
		assertEquals(0, a.location(6));
		assertEquals(2, a.location(8));
	}
	@Test public void enlarge() {
		a.enlarge();
		assertEquals(0, a.head);
		assertEquals(10, a.size);
		assertEquals(32, a.array.length);
		for (int i = 0; i < 10; ++i)
			assertEquals(i + 1, a.get(i));
	}
	@Test public void get() {
		assertEquals(1, a.get(0));
		assertEquals(6, a.get(5));
		assertEquals(10, a.get(9));
	}
	@Test(expected = IndexOutOfBoundsException.class) public void get_bounds() {
		a.get(a.size);
	}
	@Test public void set() {
		a.set(4, 100);
		a.set(8, 200);
		assertEquals(100, a.get(4));
		assertEquals(200, a.get(8));
		assertEquals(100, a.array[14]);
		assertEquals(200, a.array[2]);
	}
	@Test(expected = IndexOutOfBoundsException.class) public void set_bounds() {
		a.set(a.size, 100);
	}
	@Test public void move_left() {
		a.move(4, 2, 5);
		assertArrayEquals(new Object[] { 9, 8, 9, 10, null, null, null, null, null, null, 1, 2, 5, 6, 7, 8 }, a.array);
	}
	@Test public void move_right() {
		a.move(2, 4, 5);
		assertArrayEquals(new Object[] { 5, 6, 7, 10, null, null, null, null, null, null, 1, 2, 3, 4, 3, 4 }, a.array);
	}
	@Test public void insert_end() {
		a.insert(a.size, 3);
		assertEquals(10, a.head);
		assertEquals(13, a.size);
		assertArrayEquals(new Object[] { 7, 8, 9, 10, null, null, null, null, null, null, 1, 2, 3, 4, 5, 6 }, a.array);
	}
	@Test public void insert_right() {
		a.insert(8, 3);
		assertEquals(10, a.head);
		assertEquals(13, a.size);
		assertArrayEquals(new Object[] { 7, 8, null, null, null, 9, 10, null, null, null, 1, 2, 3, 4, 5, 6 }, a.array);
	}
	@Test public void insert_left() {
		a.insert(2, 3);
		assertEquals(7, a.head);
		assertEquals(13, a.size);
		assertArrayEquals(new Object[] { 7, 8, 9, 10, null, null, null, 1, 2, null, null, null, 3, 4, 5, 6 }, a.array);
	}
	@Test public void insert_front() {
		a.insert(0, 3);
		assertEquals(7, a.head);
		assertEquals(13, a.size);
		assertArrayEquals(new Object[] { 7, 8, 9, 10, null, null, null, null, null, null, 1, 2, 3, 4, 5, 6 }, a.array);
	}
	@Test(expected = IndexOutOfBoundsException.class) public void insert_bounds() {
		a.insert(-1, 1);
	}
	@Test(expected = IllegalArgumentException.class) public void insert_negative() {
		a.insert(5, -1);
	}
	@Test public void insert_enlarge() {
		a.insert(a.size, 200);
		assertEquals(0, a.head);
		assertEquals(210, a.size);
		assertEquals(256, a.array.length);
	}
	@Test public void remove_end() {
		a.remove(7, 3);
		assertEquals(10, a.head);
		assertEquals(7, a.size);
		assertArrayEquals(new Object[] { 7, null, null, null, null, null, null, null, null, null, 1, 2, 3, 4, 5, 6 }, a.array);
	}
	@Test public void remove_right() {
		a.remove(4, 3);
		assertEquals(10, a.head);
		assertEquals(7, a.size);
		assertArrayEquals(new Object[] { 10, null, null, null, null, null, null, null, null, null, 1, 2, 3, 4, 8, 9 }, a.array);
	}
	@Test public void remove_left() {
		a.remove(2, 3);
		assertEquals(13, a.head);
		assertEquals(7, a.size);
		assertArrayEquals(new Object[] { 7, 8, 9, 10, null, null, null, null, null, null, null, null, null, 1, 2, 6 }, a.array);
	}
	@Test public void remove_front() {
		a.remove(0, 3);
		assertEquals(13, a.head);
		assertEquals(7, a.size);
		assertArrayEquals(new Object[] { 7, 8, 9, 10, null, null, null, null, null, null, null, null, null, 4, 5, 6 }, a.array);
	}
	@Test(expected = IndexOutOfBoundsException.class) public void remove_boundsLeft() {
		a.remove(-1, 3);
	}
	@Test(expected = IndexOutOfBoundsException.class) public void remove_boundsRight() {
		a.remove(8, 3);
	}
	@Test(expected = IllegalArgumentException.class) public void remove_negative() {
		a.remove(5, -1);
	}
}
