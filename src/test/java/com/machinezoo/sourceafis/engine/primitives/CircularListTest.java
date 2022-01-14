// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.primitives;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;

public class CircularListTest {
	private final CircularList<Integer> l = new CircularList<>();
	private final ListIterator<Integer> it = l.listIterator();
	public CircularListTest() {
		for (int i = 0; i < 5; ++i)
			l.add(i + 1);
	}
	@Test
	public void add() {
		assertTrue(l.add(100));
		assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), l);
	}
	@Test
	public void addAt() {
		l.add(3, 100);
		l.add(6, 200);
		l.add(0, 300);
		assertEquals(Arrays.asList(300, 1, 2, 3, 100, 4, 5, 200), l);
	}
	@Test
	public void addAt_bounds() {
		assertThrows(IndexOutOfBoundsException.class, () -> l.add(6, 10));
	}
	@Test
	public void addAll() {
		assertFalse(l.addAll(Collections.emptyList()));
		assertTrue(l.addAll(Arrays.asList(11, 12, 13)));
		assertEquals(Arrays.asList(1, 2, 3, 4, 5, 11, 12, 13), l);
	}
	@Test
	public void addAllAt() {
		assertFalse(l.addAll(3, Collections.emptyList()));
		assertTrue(l.addAll(3, Arrays.asList(11, 12, 13)));
		assertEquals(Arrays.asList(1, 2, 3, 11, 12, 13, 4, 5), l);
	}
	@Test
	public void clear() {
		l.clear();
		assertEquals(0, l.size());
	}
	@Test
	public void contains() {
		assertTrue(l.contains(3));
		assertFalse(l.contains(10));
	}
	@Test
	public void containsAll() {
		assertTrue(l.containsAll(Arrays.asList(2, 3)));
		assertFalse(l.containsAll(Arrays.asList(1, 10)));
	}
	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void equals() {
		assertTrue(l.equals(Arrays.asList(1, 2, 3, 4, 5)));
		assertFalse(l.equals(5));
		assertFalse(l.equals(Arrays.asList(1, 2, 10, 4, 5)));
		assertFalse(l.equals(Arrays.asList(1, 2, 3, 4)));
	}
	@Test
	public void get() {
		assertEquals(2, (int)l.get(1));
		assertEquals(4, (int)l.get(3));
		assertThrows(IndexOutOfBoundsException.class, () -> l.get(5));
	}
	@Test
	public void hashCodeConsistent() {
		CircularList<Integer> o = new CircularList<>();
		assertNotEquals(o.hashCode(), l.hashCode());
		for (int i = 0; i < 5; ++i)
			o.add(i + 1);
		assertEquals(o.hashCode(), l.hashCode());
		l.add(6);
		assertNotEquals(o.hashCode(), l.hashCode());
	}
	@Test
	public void indexOf() {
		l.add(3);
		assertEquals(2, l.indexOf(3));
		assertEquals(-1, l.indexOf(10));
	}
	@Test
	public void isEmpty() {
		assertFalse(l.isEmpty());
		l.clear();
		assertTrue(l.isEmpty());
	}
	@Test
	public void iterator() {
		List<Integer> c = new ArrayList<>();
		for (Integer n : l)
			c.add(n);
		assertEquals(Arrays.asList(1, 2, 3, 4, 5), c);
	}
	@Test
	public void lastIndexOf() {
		assertEquals(2, l.lastIndexOf(3));
		l.add(3);
		assertEquals(5, l.lastIndexOf(3));
		assertEquals(-1, l.lastIndexOf(10));
	}
	@Test
	public void removeAt() {
		assertEquals(3, (int)l.remove(2));
		assertEquals(Arrays.asList(1, 2, 4, 5), l);
		assertThrows(IndexOutOfBoundsException.class, () -> l.remove(5));
	}
	@Test
	public void removeItem() {
		assertTrue(l.remove(Integer.valueOf(2)));
		assertFalse(l.remove(Integer.valueOf(10)));
		assertEquals(Arrays.asList(1, 3, 4, 5), l);
	}
	@Test
	public void removeAll() {
		l.removeAll(Arrays.asList(2, 4, 10));
		assertEquals(Arrays.asList(1, 3, 5), l);
	}
	@Test
	public void retainAll() {
		l.retainAll(Arrays.asList(2, 4, 10));
		assertEquals(Arrays.asList(2, 4), l);
	}
	@Test
	public void set() {
		assertEquals(3, (int)l.set(2, 10));
		assertEquals(Arrays.asList(1, 2, 10, 4, 5), l);
		assertThrows(IndexOutOfBoundsException.class, () -> l.set(5, 10));
	}
	@Test
	public void size() {
		assertEquals(5, l.size());
	}
	@Test
	public void subList() {
		assertThrows(UnsupportedOperationException.class, () -> l.subList(0, 1));
	}
	@Test
	public void toArray() {
		assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, l.toArray());
		assertThrows(UnsupportedOperationException.class, () -> l.toArray(new Integer[10]));
	}
	@Test
	public void toString_readable() {
		assertEquals("[1, 2, 3, 4, 5]", l.toString());
	}
	@Test
	public void listIterator() {
		assertEquals(0, l.listIterator().nextIndex());
	}
	@Test
	public void listIteratorAt() {
		assertEquals(3, l.listIterator(3).nextIndex());
		assertThrows(IndexOutOfBoundsException.class, () -> l.listIterator(6));
	}
	@Test
	public void listIterator_add() {
		assertThrows(UnsupportedOperationException.class, () -> it.add(10));
	}
	@Test
	public void listIterator_hasNext() {
		for (int i = 0; i < 5; ++i) {
			assertTrue(it.hasNext());
			it.next();
		}
		assertFalse(it.hasNext());
	}
	@Test
	public void listIterator_next() {
		for (int i = 0; i < 5; ++i)
			assertEquals(i + 1, (int)it.next());
		assertThrows(NoSuchElementException.class, () -> it.next());
	}
	@Test
	public void listIterator_nextIndex() {
		for (int i = 0; i < 5; ++i) {
			assertEquals(i, it.nextIndex());
			it.next();
		}
		assertEquals(5, it.nextIndex());
	}
	@Test
	public void listIterator_hasPrevious() {
		assertFalse(it.hasPrevious());
		for (int i = 0; i < 5; ++i) {
			it.next();
			assertTrue(it.hasPrevious());
		}
	}
	@Test
	public void listIterator_previous() {
		for (int i = 0; i < 5; ++i)
			it.next();
		for (int i = 5; i > 0; --i)
			assertEquals(i, (int)it.previous());
		assertThrows(NoSuchElementException.class, () -> it.previous());
	}
	@Test
	public void listIterator_previousIndex() {
		assertEquals(-1, it.previousIndex());
		for (int i = 0; i < 5; ++i) {
			it.next();
			assertEquals(i, it.previousIndex());
		}
	}
	@Test
	public void listIterator_remove() {
		assertThrows(UnsupportedOperationException.class, () -> it.remove());
	}
	@Test
	public void listIterator_set() {
		assertThrows(UnsupportedOperationException.class, () -> it.set(10));
	}
}
