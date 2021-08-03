// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
module com.machinezoo.sourceafis;

import java.lang;
import std.exception;

class CircularArray {
	Variant[] array;
	int head;
	int size;
	this(int capacity) {
		array = new Variant[capacity];
	}
	void validateItemIndex(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException();
	}
	void validateCursorIndex(int index) {
		if (index < 0 || index > size)
			throw new IndexOutOfBoundsException();
	}
	ulong location(int index) {
		return head + index < array.length ? head + index : head + index - array.length;
	}
	void enlarge() {
		Variant[] enlarged = new Variant[2 * array.length];
		for (int i = 0; i < size; ++i)
			enlarged[i] = array[location(i)];
		array = enlarged;
		head = 0;
	}
	Variant get(int index) {
		validateItemIndex(index);
		return array[location(index)];
	}
	void set(int index, Variant item) {
		validateItemIndex(index);
		array[location(index)] = item;
	}
	void move(int from, int to, int length) {
		if (from < to) {
			for (int i = length - 1; i >= 0; --i)
				set(to + i, get(from + i));
		} else if (from > to) {
			for (int i = 0; i < length; ++i)
				set(to + i, get(from + i));
		}
	}
	void insert(int index, int amount) {
		validateCursorIndex(index);
		if (amount < 0)
			throw new IllegalArgumentException();
		while (size + amount > array.length)
			enlarge();
		if (2 * index >= size) {
			size += amount;
			move(index, index + amount, size - amount - index);
		} else {
			head -= amount;
			size += amount;
			if (head < 0)
				head += array.length;
			move(amount, 0, index);
		}
		for (int i = 0; i < amount; ++i)
			set(index + i, Variant(null));
	}
	void remove(int index, int amount) {
		validateCursorIndex(index);
		if (amount < 0)
			throw new IllegalArgumentException();
		validateCursorIndex(index + amount);
		if (2 * index >= size - amount) {
			move(index + amount, index, size - amount - index);
			for (int i = 0; i < amount; ++i)
				set(size - i - 1, Variant(null));
			size -= amount;
		} else {
			move(0, amount, index);
			for (int i = 0; i < amount; ++i)
				set(i, Variant(null));
			head += amount;
			size -= amount;
			if (head >= array.length)
				head -= array.length;
		}
	}
}

import dunit;
import std.variant : Variant;
import std.algorithm : map;
import std.array : array;
import std.stdio : writeln;

public class CircularArrayTest {
	private CircularArray a = new CircularArray(16);
	public this() {
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
		assertThrown!IndexOutOfBoundsException(() => a.validateItemIndex(-1));
		assertThrown!IndexOutOfBoundsException(() => a.validateItemIndex(10));
	}
	@Test
	public void validateCursorIndex() {
		a.validateCursorIndex(0);
		a.validateCursorIndex(4);
		a.validateCursorIndex(10);
		assertThrown!IndexOutOfBoundsException(() => a.validateCursorIndex(-1));
		assertThrown!IndexOutOfBoundsException(() => a.validateCursorIndex(11));
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
		assertThrown!IndexOutOfBoundsException(() => a.get(a.size));
	}
	@Test
	public void set() {
		a.set(4, cast(Variant) 100);
		a.set(8, cast(Variant) 200);
		assertEquals(100, a.get(4));
		assertEquals(200, a.get(8));
		assertEquals(100, a.array[14]);
		assertEquals(200, a.array[2]);
		assertThrown!IndexOutOfBoundsException(() => a.set(a.size, cast(Variant) 100));
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
		assertThrown!IndexOutOfBoundsException(() => a.insert(-1, 1));
	}
	@Test
	public void insert_negative() {
		assertThrown!IllegalArgumentException(() => a.insert(5, -1));
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
		assertThrown!IndexOutOfBoundsException(() => a.remove(-1, 3));
	}
	@Test
	public void remove_boundsRight() {
		assertThrown!IndexOutOfBoundsException(() => a.remove(8, 3));
	}
	@Test
	public void remove_negative() {
		assertThrown!IllegalArgumentException(() => a.remove(5, -1));
                writeln("FOOBAR");
	}
}

