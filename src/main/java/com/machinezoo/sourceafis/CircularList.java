// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.util.*;

class CircularList<T> implements List<T> {
	private final CircularArray inner = new CircularArray(16);
	@Override public boolean add(T item) {
		inner.insert(inner.size, 1);
		inner.set(inner.size - 1, item);
		return true;
	}
	@Override public void add(int index, T item) {
		inner.insert(index, 1);
		inner.set(index, item);
	}
	@Override public boolean addAll(Collection<? extends T> collection) {
		for (T item : collection)
			add(item);
		return !collection.isEmpty();
	}
	@Override public boolean addAll(int index, Collection<? extends T> collection) {
		inner.insert(index, collection.size());
		for (T item : collection) {
			inner.set(index, item);
			++index;
		}
		return !collection.isEmpty();
	}
	@Override public void clear() {
		inner.remove(0, inner.size);
	}
	@Override public boolean contains(Object item) {
		for (int i = 0; i < size(); ++i)
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
		if (size() != other.size())
			return false;
		for (int i = 0; i < size(); ++i)
			if (!Objects.equals(get(i), other.get(i)))
				return false;
		return true;
	}
	@SuppressWarnings("unchecked") @Override public T get(int index) {
		return (T)inner.get(index);
	}
	@Override public int hashCode() {
		int hash = 1;
		for (int i = 0; i < size(); ++i)
			hash = 31 * hash + Objects.hashCode(inner.get(i));
		return hash;
	}
	@Override public int indexOf(Object item) {
		for (int i = 0; i < size(); ++i)
			if (Objects.equals(get(i), item))
				return i;
		return -1;
	}
	@Override public boolean isEmpty() {
		return size() == 0;
	}
	@Override public Iterator<T> iterator() {
		return new ArrayIterator();
	}
	@Override public int lastIndexOf(Object item) {
		for (int i = size() - 1; i >= 0; --i)
			if (Objects.equals(get(i), item))
				return i;
		return -1;
	}
	@Override public ListIterator<T> listIterator() {
		return new ArrayIterator();
	}
	@Override public ListIterator<T> listIterator(int index) {
		inner.validateCursorIndex(index);
		ArrayIterator iterator = new ArrayIterator();
		iterator.index = index;
		return iterator;
	}
	@Override public T remove(int index) {
		T result = get(index);
		inner.remove(index, 1);
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
		for (int i = size() - 1; i >= 0; --i)
			if (!collection.contains(get(i))) {
				remove(i);
				changed = true;
			}
		return changed;
	}
	@Override public T set(int index, T element) {
		T previous = get(index);
		inner.set(index, element);
		return previous;
	}
	@Override public int size() {
		return inner.size;
	}
	@Override public List<T> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}
	@Override public Object[] toArray() {
		Object[] array = new Object[size()];
		for (int i = 0; i < size(); ++i)
			array[i] = get(i);
		return array;
	}
	@Override public <U> U[] toArray(U[] array) {
		throw new UnsupportedOperationException();
	}
	@Override public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("[");
		for (int i = 0; i < inner.size; ++i) {
			if (i > 0)
				s.append(", ");
			s.append(Objects.toString(inner.get(i)));
		}
		s.append("]");
		return s.toString();
	}
	private class ArrayIterator implements ListIterator<T> {
		int index = 0;
		@Override public void add(T e) {
			throw new UnsupportedOperationException();
		}
		@Override public boolean hasNext() {
			return index < size();
		}
		@Override public T next() {
			if (index >= size())
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
