// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.internal;

import java.util.*;

public class CircularList<T> implements List<T> {
	private Object[] inner;
	private int first;
	private int size;
	public CircularList() {
		inner = new Object[16];
	}
	private void validateItemIndex(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException();
	}
	private void validateCursorIndex(int index) {
		if (index < 0 || index > size)
			throw new IndexOutOfBoundsException();
	}
	private int realIndex(int index) {
		return first + index < inner.length ? first + index : first + index - inner.length;
	}
	private void incFirst() {
		++first;
		if (first >= inner.length)
			first -= inner.length;
	}
	private void decFirst() {
		--first;
		if (first < 0)
			first += inner.length;
	}
	private void enlarge() {
		Object[] enlarged = new Object[2 * inner.length];
		for (int i = 0; i < size; ++i)
			enlarged[i] = inner[realIndex(i)];
		inner = enlarged;
		first = 0;
	}
	private void move(int from, int to, int length) {
		if (from < to) {
			for (int i = length - 1; i >= 0; --i)
				inner[realIndex(to + i)] = inner[realIndex(from + i)];
		} else if (from > to) {
			for (int i = 0; i < length; ++i)
				inner[realIndex(to + i)] = inner[realIndex(from + i)];
		}
	}
	private void moveForward(int from, int length, int steps) {
		move(from, from + steps, length);
	}
	private void moveBackward(int from, int length, int steps) {
		move(from, from - steps, length);
	}
	private void insertSpaceForward(int index, int space) {
		size += space;
		moveForward(index, size - index - space, space);
	}
	private void insertSpaceBackward(int index, int space) {
		for (int i = 0; i < space; ++i)
			decFirst();
		size += space;
		moveBackward(space, index, space);
	}
	private void insertSpace(int index, int space) {
		while (size + space > inner.length)
			enlarge();
		if (index >= size / 2)
			insertSpaceForward(index, space);
		else
			insertSpaceBackward(index, space);
	}
	private void removeSpaceForward(int index, int space) {
		moveBackward(index + space, size - index - space, space);
		size -= space;
	}
	private void removeSpaceBackward(int index, int space) {
		moveForward(0, index, space);
		for (int i = 0; i < space; ++i)
			incFirst();
		size += space;
	}
	private void removeSpace(int index, int space) {
		if (index >= size / 2)
			removeSpaceForward(index, space);
		else
			removeSpaceBackward(index, space);
	}
	@Override public boolean add(T item) {
		if (size >= inner.length)
			enlarge();
		++size;
		inner[realIndex(size - 1)] = item;
		return true;
	}
	@Override public void add(int index, T item) {
		validateCursorIndex(index);
		insertSpace(index, 1);
		inner[realIndex(index)] = item;
	}
	@Override public boolean addAll(Collection<? extends T> collection) {
		for (T item : collection)
			add(item);
		return !collection.isEmpty();
	}
	@Override public boolean addAll(int index, Collection<? extends T> collection) {
		validateCursorIndex(index);
		insertSpace(index, collection.size());
		for (T item : collection) {
			inner[realIndex(index)] = item;
			++index;
		}
		return !collection.isEmpty();
	}
	@Override public void clear() {
		first = 0;
		size = 0;
	}
	@Override public boolean contains(Object item) {
		for (int i = 0; i < size; ++i)
			if (Objects.equals(get(i), item))
				return true;
		return false;
	}
	@Override public boolean containsAll(Collection<?> collection) {
		for (Object item : collection)
			if (!contains(item))
				return false;
		return true;
	}
	@Override public boolean equals(Object obj) {
		if (!(obj instanceof List<?>))
			return false;
		List<?> other = (List<?>)obj;
		if (size != other.size())
			return false;
		for (int i = 0; i < size; ++i)
			if (!Objects.equals(get(i), other.get(i)))
				return false;
		return true;
	}
	@SuppressWarnings("unchecked") @Override public T get(int index) {
		validateItemIndex(index);
		return (T)inner[realIndex(index)];
	}
	@Override public int hashCode() {
		int hash = 1;
		for (int i = 0; i < size; ++i)
			hash = 31 * hash + Objects.hashCode(inner[realIndex(i)]);
		return hash;
	}
	@Override public int indexOf(Object item) {
		for (int i = 0; i < size; ++i)
			if (Objects.equals(get(i), item))
				return i;
		return -1;
	}
	@Override public boolean isEmpty() {
		return size == 0;
	}
	@Override public Iterator<T> iterator() {
		return new ArrayIterator();
	}
	@Override public int lastIndexOf(Object item) {
		for (int i = size - 1; i >= 0; --i)
			if (Objects.equals(get(i), item))
				return i;
		return -1;
	}
	@Override public ListIterator<T> listIterator() {
		return new ArrayIterator();
	}
	@Override public ListIterator<T> listIterator(int index) {
		ArrayIterator iterator = new ArrayIterator();
		iterator.index = index;
		return iterator;
	}
	@Override public T remove(int index) {
		T result = get(index);
		removeSpace(index, 1);
		return result;
	}
	@Override public boolean remove(Object item) {
		int index = indexOf(item);
		if (index >= 0) {
			remove(index);
			return true;
		} else
			return false;
	}
	@Override public boolean removeAll(Collection<?> collection) {
		boolean changed = false;
		for (Object item : collection)
			changed |= remove(item);
		return changed;
	}
	@Override public boolean retainAll(Collection<?> collection) {
		boolean changed = false;
		for (int i = size - 1; i >= 0; --i)
			if (!collection.contains(get(i))) {
				remove(i);
				changed = true;
			}
		return changed;
	}
	@Override public T set(int index, T element) {
		T previous = get(index);
		inner[realIndex(index)] = element;
		return previous;
	}
	@Override public int size() {
		return size;
	}
	@Override public List<T> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}
	@Override public Object[] toArray() {
		Object[] array = new Object[size];
		for (int i = 0; i < size; ++i)
			array[i] = get(i);
		return array;
	}
	@Override public <U> U[] toArray(U[] array) {
		throw new UnsupportedOperationException();
	}
	class ArrayIterator implements ListIterator<T> {
		int index = 0;
		@Override public void add(T e) {
			throw new UnsupportedOperationException();
		}
		@Override public boolean hasNext() {
			return index < size;
		}
		@Override public T next() {
			if (index >= size)
				throw new NoSuchElementException();
			++index;
			return get(index - 1);
		}
		@Override public int nextIndex() {
			return index;
		}
		@Override public boolean hasPrevious() {
			return index > 0;
		}
		@Override public T previous() {
			if (index <= 0)
				throw new NoSuchElementException();
			--index;
			return get(index);
		}
		@Override public int previousIndex() {
			return index - 1;
		}
		@Override public void remove() {
			throw new UnsupportedOperationException();
		}
		@Override public void set(T e) {
			throw new UnsupportedOperationException();
		}
	}
}
