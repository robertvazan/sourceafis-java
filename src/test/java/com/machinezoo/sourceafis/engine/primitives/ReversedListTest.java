// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.primitives;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;

public class ReversedListTest {
	private final List<Integer> o = new ArrayList<>();
	private final List<Integer> r = new ReversedList<>(o);
	private final ListIterator<Integer> it = r.listIterator();
	public ReversedListTest() {
		for (int i = 0; i < 5; ++i)
			o.add(i + 1);
	}
	@Test
	public void add() {
		assertTrue(r.add(10));
		assertEquals(Arrays.asList(10, 1, 2, 3, 4, 5), o);
	}
	@Test
	public void addAt() {
		r.add(1, 10);
		r.add(6, 20);
		r.add(0, 30);
		assertEquals(Arrays.asList(20, 1, 2, 3, 4, 10, 5, 30), o);
	}
	@Test
	public void addAt_bounds() {
		assertThrows(IndexOutOfBoundsException.class, () -> r.add(6, 10));
	}
	@Test
	public void addAll() {
		assertTrue(r.addAll(Arrays.asList(10, 20, 30)));
		assertFalse(r.addAll(Collections.emptyList()));
		assertEquals(Arrays.asList(30, 20, 10, 1, 2, 3, 4, 5), o);
	}
	@Test
	public void addAllAt() {
		assertTrue(r.addAll(1, Arrays.asList(10, 20, 30)));
		assertFalse(r.addAll(1, Collections.emptyList()));
		assertEquals(Arrays.asList(1, 2, 3, 4, 30, 20, 10, 5), o);
	}
	@Test
	public void addAllAt_bounds() {
		assertThrows(IndexOutOfBoundsException.class, () -> r.addAll(6, Arrays.asList(10)));
	}
	@Test
	public void clear() {
		r.clear();
		assertTrue(o.isEmpty());
	}
	@Test
	public void contains() {
		assertTrue(r.contains(3));
		assertFalse(r.contains(10));
	}
	@Test
	public void containsAll() {
		assertTrue(r.containsAll(Arrays.asList(2, 4)));
		assertFalse(r.containsAll(Arrays.asList(2, 10)));
	}
	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void equals() {
		assertTrue(r.equals(Arrays.asList(5, 4, 3, 2, 1)));
		assertFalse(r.equals(Arrays.asList(1, 2, 3, 4, 5)));
		assertFalse(r.equals(Arrays.asList(5, 4, 3, 2)));
		assertFalse(r.equals(3));
	}
	@Test
	public void get() {
		assertEquals(4, (int)r.get(1));
		assertEquals(2, (int)r.get(3));
		assertThrows(IndexOutOfBoundsException.class, () -> r.get(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> r.get(5));
	}
	@Test
	public void hashCodePass() {
		assertEquals(o.hashCode(), r.hashCode());
	}
	@Test
	public void indexOf() {
		r.add(4);
		assertEquals(1, r.indexOf(4));
		assertEquals(-1, r.indexOf(10));
	}
	@Test
	public void isEmpty() {
		assertFalse(r.isEmpty());
		o.clear();
		assertTrue(r.isEmpty());
	}
	@Test
	public void iterator() {
		List<Integer> c = new ArrayList<>();
		for (Integer n : r)
			c.add(n);
		assertEquals(Arrays.asList(5, 4, 3, 2, 1), c);
	}
	@Test
	public void lastIndexOf() {
		assertEquals(1, r.lastIndexOf(4));
		r.add(4);
		assertEquals(5, r.lastIndexOf(4));
		assertEquals(-1, r.lastIndexOf(10));
	}
	@Test
	public void listIterator() {
		assertEquals(0, r.listIterator().nextIndex());
	}
	@Test
	public void listIteratorAt() {
		assertEquals(3, r.listIterator(3).nextIndex());
		assertThrows(IndexOutOfBoundsException.class, () -> r.listIterator(6));
	}
	@Test
	public void removeAt() {
		assertEquals(4, (int)r.remove(1));
		assertEquals(Arrays.asList(5, 3, 2, 1), r);
	}
	@Test
	public void removeAt_bounds() {
		assertThrows(IndexOutOfBoundsException.class, () -> r.remove(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> r.remove(5));
	}
	@Test
	public void removeItem() {
		r.add(4);
		assertTrue(r.remove(Integer.valueOf(4)));
		assertFalse(r.remove(Integer.valueOf(10)));
		assertEquals(Arrays.asList(5, 3, 2, 1, 4), r);
	}
	@Test
	public void removeAll() {
		r.removeAll(Arrays.asList(2, 4, 10));
		assertEquals(Arrays.asList(5, 3, 1), r);
	}
	@Test
	public void retainAll() {
		r.retainAll(Arrays.asList(2, 4, 10));
		assertEquals(Arrays.asList(4, 2), r);
	}
	@Test
	public void set() {
		r.set(1, 10);
		assertEquals(Arrays.asList(5, 10, 3, 2, 1), r);
		assertThrows(IndexOutOfBoundsException.class, () -> r.set(-1, 10));
		assertThrows(IndexOutOfBoundsException.class, () -> r.set(5, 10));
	}
	@Test
	public void size() {
		assertEquals(5, r.size());
	}
	@Test
	public void subList() {
		List<Integer> s = r.subList(1, 4);
		assertEquals(Arrays.asList(4, 3, 2), s);
		s.remove(0);
		assertEquals(Arrays.asList(5, 3, 2, 1), r);
		s.add(10);
		assertEquals(Arrays.asList(5, 3, 2, 10, 1), r);
	}
	@Test
	public void toArray() {
		assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, r.toArray());
		assertThrows(UnsupportedOperationException.class, () -> r.toArray(new Integer[10]));
	}
	@Test
	public void toString_readable() {
		assertEquals("[5, 4, 3, 2, 1]", r.toString());
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
			assertEquals(5 - i, (int)it.next());
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
		for (int i = 0; i < 5; ++i)
			assertEquals(i + 1, (int)it.previous());
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
