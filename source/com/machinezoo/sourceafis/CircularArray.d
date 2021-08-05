// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
module source.com.machinezoo.sourceafis;

import java.lang : IndexOutOfBoundsException, IllegalArgumentException;

import std.variant : Variant;

public class CircularArray {
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
