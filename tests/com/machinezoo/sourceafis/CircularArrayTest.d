// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
module tests.com.machinezoo.sourceafis;

import source.com.machinezoo.sourceafis : CircularArray;
import java.lang : IndexOutOfBoundsException, IllegalArgumentException;

import std.stdio : writeln;
import std.variant : Variant;
import std.exception : assertThrown;

import dunit;

public class CircularArrayTest {
        mixin UnitTest;

	private CircularArray a = new CircularArray(16);
	public this() {
                a = new CircularArray(16);
		a.insert(0, 16);
		a.remove(0, 10);
		a.insert(a.size, 4);
		for (int i = 0; i < a.size; ++i)
			a.set(i, cast(Variant) i + 1);
	}
	@Test
	public void constructor() {
		CircularArray a = new CircularArray(13);
		assertEquals(13, a.array.length);
		assertEquals(0, a.size);
		assertEquals(0, a.head);
	}
	@Test
	public void validateItemIndex() {
		a.validateItemIndex(0);
		a.validateItemIndex(5);
		a.validateItemIndex(9);
		assertThrown!IndexOutOfBoundsException(a.validateItemIndex(-1));
		assertThrown!IndexOutOfBoundsException(a.validateItemIndex(10));
	}
	@Test
	public void validateCursorIndex() {
		a.validateCursorIndex(0);
		a.validateCursorIndex(4);
		a.validateCursorIndex(10);
		assertThrown!IndexOutOfBoundsException(a.validateCursorIndex(-1));
		assertThrown!IndexOutOfBoundsException(a.validateCursorIndex(11));
	}
	@Test
	public void location() {
		assertEquals(10, a.location(0));
		assertEquals(14, a.location(4));
		assertEquals(15, a.location(5));
		assertEquals(0, a.location(6));
		assertEquals(2, a.location(8));
	}
	@Test
	public void enlarge() {
		a.enlarge();
		assertEquals(0, a.head);
		assertEquals(10, a.size);
		assertEquals(32, a.array.length);
		for (int i = 0; i < 10; ++i)
			assertEquals(i + 1, a.get(i));
	}
	@Test
	public void get() {
		assertEquals(1, a.get(0));
		assertEquals(6, a.get(5));
		assertEquals(10, a.get(9));
		assertThrown!IndexOutOfBoundsException(a.get(a.size));
	}
	@Test
	public void set() {
		a.set(4, cast(Variant) 100);
		a.set(8, cast(Variant) 200);
		assertEquals(100, a.get(4));
		assertEquals(200, a.get(8));
		assertEquals(100, a.array[14]);
		assertEquals(200, a.array[2]);
		assertThrown!IndexOutOfBoundsException(a.set(a.size, cast(Variant) 100));
	}
	@Test
	public void move_left() {
		a.move(4, 2, 5);
                Variant[] testInput = [
                    Variant(9), Variant(8), Variant(9), Variant(10), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(1), Variant(2), Variant(5), Variant(6), Variant(7), Variant(8)
                ];
		assertArrayEquals(testInput, a.array);
	}
	@Test
	public void move_right() {
		a.move(2, 4, 5);
		assertArrayEquals([
                    Variant(5), Variant(6), Variant(7), Variant(10), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(1), Variant(2), Variant(3), Variant(4), Variant(3), Variant(4)
                ], a.array);
	}
	@Test
	public void insert_end() {
		a.insert(a.size, 3);
		assertEquals(10, a.head);
		assertEquals(13, a.size);
		assertArrayEquals([
                    Variant(7), Variant(8), Variant(9), Variant(10), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(1), Variant(2), Variant(3), Variant(4), Variant(5), Variant(6)
                ], a.array);
	}
	@Test
	public void insert_right() {
		a.insert(8, 3);
		assertEquals(10, a.head);
		assertEquals(13, a.size);
		assertArrayEquals([
                    Variant(7), Variant(8), Variant(null), Variant(null), Variant(null), Variant(9), Variant(10), Variant(null), Variant(null), Variant(null), Variant(1), Variant(2), Variant(3), Variant(4), Variant(5), Variant(6)
                ], a.array);
	}
	@Test
	public void insert_left() {
		a.insert(2, 3);
		assertEquals(7, a.head);
		assertEquals(13, a.size);
		assertArrayEquals([
                    Variant(7), Variant(8), Variant(9), Variant(10), Variant(null), Variant(null), Variant(null), Variant(1), Variant(2), Variant(null), Variant(null), Variant(null), Variant(3), Variant(4), Variant(5), Variant(6)
                ], a.array);
	}
	@Test
	public void insert_front() {
		a.insert(0, 3);
		assertEquals(7, a.head);
		assertEquals(13, a.size);
		assertArrayEquals([
                    Variant(7), Variant(8), Variant(9), Variant(10), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(1), Variant(2), Variant(3), Variant(4), Variant(5), Variant(6)
                ], a.array);
	}
	@Test
	public void insert_bounds() {
		assertThrown!IndexOutOfBoundsException(a.insert(-1, 1));
	}
	@Test
	public void insert_negative() {
		assertThrown!IllegalArgumentException(a.insert(5, -1));
	}
	@Test
	public void insert_enlarge() {
		a.insert(a.size, 200);
		assertEquals(0, a.head);
		assertEquals(210, a.size);
		assertEquals(256, a.array.length);
	}
	@Test
	public void remove_end() {
		a.remove(7, 3);
		assertEquals(10, a.head);
		assertEquals(7, a.size);
		assertArrayEquals([
                    Variant(7), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(1), Variant(2), Variant(3), Variant(4), Variant(5), Variant(6)
                ], a.array);
	}
	@Test
	public void remove_right() {
		a.remove(4, 3);
		assertEquals(10, a.head);
		assertEquals(7, a.size);
		assertArrayEquals([
                    Variant(10), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(1), Variant(2), Variant(3), Variant(4), Variant(8), Variant(9)
                ], a.array);
	}
	@Test
	public void remove_left() {
		a.remove(2, 3);
		assertEquals(13, a.head);
		assertEquals(7, a.size);
		assertArrayEquals([
                    Variant(7), Variant(8), Variant(9), Variant(10), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(1), Variant(2), Variant(6)
                ], a.array);
	}
	@Test
	public void remove_front() {
		a.remove(0, 3);
		assertEquals(13, a.head);
		assertEquals(7, a.size);
		assertArrayEquals([
                    Variant(7), Variant(8), Variant(9), Variant(10), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(null), Variant(4), Variant(5), Variant(6)
                ], a.array);
	}
	@Test
	public void remove_boundsLeft() {
		assertThrown!IndexOutOfBoundsException(a.remove(-1, 3));
	}
	@Test
	public void remove_boundsRight() {
		assertThrown!IndexOutOfBoundsException(a.remove(8, 3));
	}
	@Test
	public void remove_negative() {
		assertThrown!IllegalArgumentException(a.remove(5, -1));
	}
}

