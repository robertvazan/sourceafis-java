// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.util.*;

class ReversedList<T> implements List<T> {
	private final List<T> inner;
	ReversedList(List<T> inner) {
		this.inner = inner;
	}
	@Override public boolean add(T item) {
		inner.add(0, item);
		return true;
	}
	@Override public void add(int index, T item) {
		inner.add(size() - index, item);
	}
	@Override public boolean addAll(Collection<? extends T> collection) {
		return inner.addAll(0, new ReversedList<>(new ArrayList<>(collection)));
	}
	@Override public boolean addAll(int index, Collection<? extends T> collection) {
		return inner.addAll(size() - index, new ReversedList<>(new ArrayList<>(collection)));
	}
	@Override public void clear() {
		inner.clear();
	}
	@Override public boolean contains(Object item) {
		return inner.contains(item);
	}
	@Override public boolean containsAll(Collection<?> collection) {
		return inner.containsAll(collection);
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
	@Override public T get(int index) {
		return inner.get(size() - index - 1);
	}
	@Override public int hashCode() {
		return inner.hashCode();
	}
	@Override public int indexOf(Object item) {
		int index = inner.lastIndexOf(item);
		return index >= 0 ? size() - index - 1 : -1;
	}
	@Override public boolean isEmpty() {
		return inner.isEmpty();
	}
	@Override public Iterator<T> iterator() {
		return new ReversedIterator();
	}
	@Override public int lastIndexOf(Object item) {
		int index = inner.indexOf(item);
		return index >= 0 ? size() - index - 1 : -1;
	}
	@Override public ListIterator<T> listIterator() {
		return new ReversedIterator();
	}
	@Override public ListIterator<T> listIterator(int index) {
		if (index < 0 || index > size())
			throw new IndexOutOfBoundsException();
		ReversedIterator iterator = new ReversedIterator();
		iterator.index = index;
		return iterator;
	}
	@Override public T remove(int index) {
		return inner.remove(size() - index - 1);
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
		return inner.removeAll(collection);
	}
	@Override public boolean retainAll(Collection<?> collection) {
		return inner.retainAll(collection);
	}
	@Override public T set(int index, T item) {
		return inner.set(size() - index - 1, item);
	}
	@Override public int size() {
		return inner.size();
	}
	@Override public List<T> subList(int fromIndex, int toIndex) {
		return new ReversedList<>(inner.subList(size() - toIndex, size() - fromIndex));
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
		for (int i = 0; i < size(); ++i) {
			if (i > 0)
				s.append(", ");
			s.append(Objects.toString(get(i)));
		}
		s.append("]");
		return s.toString();
	}
	private class ReversedIterator implements ListIterator<T> {
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
