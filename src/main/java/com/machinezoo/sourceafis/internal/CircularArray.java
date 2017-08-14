package com.machinezoo.sourceafis.internal;

class CircularArray {
	Object[] array = new Object[16];
	int head;
	int size;
	void validateItemIndex(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException();
	}
	void validateCursorIndex(int index) {
		if (index < 0 || index > size)
			throw new IndexOutOfBoundsException();
	}
	int location(int index) {
		return head + index < array.length ? head + index : head + index - array.length;
	}
	void enlarge() {
		Object[] enlarged = new Object[2 * array.length];
		for (int i = 0; i < size; ++i)
			enlarged[i] = array[location(i)];
		array = enlarged;
		head = 0;
	}
	Object get(int index) {
		validateItemIndex(index);
		return array[location(index)];
	}
	void set(int index, Object item) {
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
		while (size + amount > array.length)
			enlarge();
		if (2 * index >= size) {
			size += amount;
			move(index, index + amount, size - amount - index);
		} else {
			head -= amount;
			if (head < 0)
				head += array.length;
			move(amount, 0, index);
		}
	}
	void remove(int index, int amount) {
		validateCursorIndex(index);
		validateCursorIndex(index + amount);
		if (2 * index >= size - amount) {
			move(index + amount, index, size - amount - index);
			size -= amount;
		} else {
			move(0, amount, index);
			head += amount;
			if (head >= array.length)
				head -= array.length;
		}
	}
}
