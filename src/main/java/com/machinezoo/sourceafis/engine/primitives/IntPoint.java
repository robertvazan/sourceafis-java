// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.primitives;

import java.util.*;

public class IntPoint implements Iterable<IntPoint>, Comparable<IntPoint> {
	public static final IntPoint ZERO = new IntPoint(0, 0);
	public static final IntPoint[] EDGE_NEIGHBORS = new IntPoint[] {
		new IntPoint(0, -1),
		new IntPoint(-1, 0),
		new IntPoint(1, 0),
		new IntPoint(0, 1)
	};
	public static final IntPoint[] CORNER_NEIGHBORS = new IntPoint[] {
		new IntPoint(-1, -1),
		new IntPoint(0, -1),
		new IntPoint(1, -1),
		new IntPoint(-1, 0),
		new IntPoint(1, 0),
		new IntPoint(-1, 1),
		new IntPoint(0, 1),
		new IntPoint(1, 1)
	};
	public final int x;
	public final int y;
	public IntPoint(int x, int y) {
		this.x = x;
		this.y = y;
	}
	public int area() {
		return x * y;
	}
	public int lengthSq() {
		return Integers.sq(x) + Integers.sq(y);
	}
	public boolean contains(IntPoint other) {
		return other.x >= 0 && other.y >= 0 && other.x < x && other.y < y;
	}
	public IntPoint plus(IntPoint other) {
		return new IntPoint(x + other.x, y + other.y);
	}
	public IntPoint minus(IntPoint other) {
		return new IntPoint(x - other.x, y - other.y);
	}
	public IntPoint negate() {
		return new IntPoint(-x, -y);
	}
	public DoublePoint toDouble() {
		return new DoublePoint(x, y);
	}
	public IntPoint[] lineTo(IntPoint to) {
		IntPoint[] result;
		IntPoint relative = to.minus(this);
		if (Math.abs(relative.x) >= Math.abs(relative.y)) {
			result = new IntPoint[Math.abs(relative.x) + 1];
			if (relative.x > 0) {
				for (int i = 0; i <= relative.x; ++i)
					result[i] = new IntPoint(x + i, y + (int)Math.round(i * (relative.y / (double)relative.x)));
			} else if (relative.x < 0) {
				for (int i = 0; i <= -relative.x; ++i)
					result[i] = new IntPoint(x - i, y - (int)Math.round(i * (relative.y / (double)relative.x)));
			} else
				result[0] = this;
		} else {
			result = new IntPoint[Math.abs(relative.y) + 1];
			if (relative.y > 0) {
				for (int i = 0; i <= relative.y; ++i)
					result[i] = new IntPoint(x + (int)Math.round(i * (relative.x / (double)relative.y)), y + i);
			} else if (relative.y < 0) {
				for (int i = 0; i <= -relative.y; ++i)
					result[i] = new IntPoint(x - (int)Math.round(i * (relative.x / (double)relative.y)), y - i);
			} else
				result[0] = this;
		}
		return result;
	}
	private List<Object> fields() {
		return Arrays.asList(x, y);
	}
	@Override
	public boolean equals(Object obj) {
		return obj instanceof IntPoint && fields().equals(((IntPoint)obj).fields());
	}
	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}
	@Override
	public int compareTo(IntPoint other) {
		int resultY = Integer.compare(y, other.y);
		if (resultY != 0)
			return resultY;
		return Integer.compare(x, other.x);
	}
	@Override
	public String toString() {
		return String.format("[%d,%d]", x, y);
	}
	@Override
	public Iterator<IntPoint> iterator() {
		return new IntPointIterator();
	}
	private class IntPointIterator implements Iterator<IntPoint> {
		int atX;
		int atY;
		@Override
		public boolean hasNext() {
			return atY < y && atX < x;
		}
		@Override
		public IntPoint next() {
			if (!hasNext())
				throw new NoSuchElementException();
			IntPoint result = new IntPoint(atX, atY);
			++atX;
			if (atX >= x) {
				atX = 0;
				++atY;
			}
			return result;
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
