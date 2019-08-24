// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.util.*;

class IntRect implements Iterable<IntPoint> {
	final int x;
	final int y;
	final int width;
	final int height;
	IntRect(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	IntRect(IntPoint size) {
		this(0, 0, size.x, size.y);
	}
	int left() {
		return x;
	}
	int top() {
		return y;
	}
	int right() {
		return x + width;
	}
	int bottom() {
		return y + height;
	}
	int area() {
		return width * height;
	}
	static IntRect between(int startX, int startY, int endX, int endY) {
		return new IntRect(startX, startY, endX - startX, endY - startY);
	}
	static IntRect between(IntPoint start, IntPoint end) {
		return between(start.x, start.y, end.x, end.y);
	}
	static IntRect around(int x, int y, int radius) {
		return between(x - radius, y - radius, x + radius + 1, y + radius + 1);
	}
	static IntRect around(IntPoint center, int radius) {
		return around(center.x, center.y, radius);
	}
	IntPoint center() {
		return new IntPoint((right() + left()) / 2, (top() + bottom()) / 2);
	}
	IntRect intersect(IntRect other) {
		return between(
			new IntPoint(Math.max(left(), other.left()), Math.max(top(), other.top())),
			new IntPoint(Math.min(right(), other.right()), Math.min(bottom(), other.bottom())));
	}
	IntRect move(IntPoint delta) {
		return new IntRect(x + delta.x, y + delta.y, width, height);
	}
	private List<Object> fields() {
		return Arrays.asList(x, y, width, height);
	}
	@Override public boolean equals(Object obj) {
		return obj instanceof IntRect && fields().equals(((IntRect)obj).fields());
	}
	@Override public int hashCode() {
		return Objects.hash(x, y, width, height);
	}
	@Override public String toString() {
		return String.format("[%d,%d] @ [%d,%d]", width, height, x, y);
	}
	@Override public Iterator<IntPoint> iterator() {
		return new BlockIterator();
	}
	private class BlockIterator implements Iterator<IntPoint> {
		int atX;
		int atY;
		@Override public boolean hasNext() {
			return atY < height && atX < width;
		}
		@Override public IntPoint next() {
			if (!hasNext())
				throw new NoSuchElementException();
			IntPoint result = new IntPoint(x + atX, y + atY);
			++atX;
			if (atX >= width) {
				atX = 0;
				++atY;
			}
			return result;
		}
		@Override public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
