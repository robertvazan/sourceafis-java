package sourceafis.scalars;

import java.util.*;

public class Point implements Iterable<Point> {
	private static final Point[] edgeNeighbors = new Point[] {
		new Point(0, -1),
		new Point(-1, 0),
		new Point(1, 0),
		new Point(0, 1)
	};
	private static final Point[] cornerNeighbors = new Point[] {
		new Point(-1, -1),
		new Point(0, -1),
		new Point(1, -1),
		new Point(-1, 0),
		new Point(1, 0),
		new Point(-1, 1),
		new Point(0, 1),
		new Point(1, 1)
	};
	public final int x;
	public final int y;
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	public FPoint toFloat() {
		return new FPoint(this);
	}
	public int area() {
		return x * y;
	}
	public int lengthSq() {
		return Integers.sq(x) + Integers.sq(y);
	}
	public boolean contains(Point other) {
		return other.x >= 0 && other.y >= 0 && other.x < x && other.y < y;
	}
	public Point plus(Point other) {
		return new Point(x + other.x, y + other.y);
	}
	public Point minus(Point other) {
		return new Point(x - other.x, y - other.y);
	}
	public Point negate() {
		return new Point(-x, -y);
	}
	public Point[] lineTo(Point to) {
		Point[] result;
		Point relative = to.minus(this);
		if (Math.abs(relative.x) >= Math.abs(relative.y)) {
			result = new Point[Math.abs(relative.x) + 1];
			if (relative.x > 0) {
				for (int i = 0; i <= relative.x; ++i)
					result[i] = new Point(x + i, y + (int)Math.round(i * (relative.y / (double)relative.x)));
			} else if (relative.x < 0) {
				for (int i = 0; i <= -relative.x; ++i)
					result[i] = new Point(x - i, y - (int)Math.round(i * (relative.y / (double)relative.x)));
			} else
				result[0] = this;
		} else {
			result = new Point[Math.abs(relative.y) + 1];
			if (relative.y > 0) {
				for (int i = 0; i <= relative.y; ++i)
					result[i] = new Point(x + (int)Math.round(i * (relative.x / (double)relative.y)), y + i);
			} else if (relative.y < 0) {
				for (int i = 0; i <= -relative.y; ++i)
					result[i] = new Point(x - (int)Math.round(i * (relative.x / (double)relative.y)), y - i);
			} else
				result[0] = this;
		}
		return result;
	}
	public boolean equals(Point other) {
		return x == other.x && y == other.y;
	}
	@Override public boolean equals(Object obj) {
		return obj instanceof Point && equals((Point)obj);
	}
	@Override public int hashCode() {
		return (x << 5) + (y - x);
	}
	@Override public String toString() {
		return String.format("[%d,%d]", x, y);
	}
	@Override public Iterator<Point> iterator() {
		return new PointIterator();
	}
	public static Point[] edgeNeighbors() {
		return edgeNeighbors;
	}
	public static Point[] cornerNeighbors() {
		return cornerNeighbors;
	}
	private class PointIterator implements Iterator<Point> {
		int atX;
		int atY;
		@Override public boolean hasNext() {
			return atY < y && atX < x;
		}
		@Override public Point next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Point result = new Point(atX, atY);
			++atX;
			if (atX >= x) {
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
