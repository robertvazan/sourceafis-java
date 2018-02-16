// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.nio.*;
import java.util.*;

class Cell implements Iterable<Cell> {
	static final Cell zero = new Cell(0, 0);
	static final Cell[] edgeNeighbors = new Cell[] {
		new Cell(0, -1),
		new Cell(-1, 0),
		new Cell(1, 0),
		new Cell(0, 1)
	};
	static final Cell[] cornerNeighbors = new Cell[] {
		new Cell(-1, -1),
		new Cell(0, -1),
		new Cell(1, -1),
		new Cell(-1, 0),
		new Cell(1, 0),
		new Cell(-1, 1),
		new Cell(0, 1),
		new Cell(1, 1)
	};
	final int x;
	final int y;
	Cell(int x, int y) {
		this.x = x;
		this.y = y;
	}
	int area() {
		return x * y;
	}
	int lengthSq() {
		return Integers.sq(x) + Integers.sq(y);
	}
	boolean contains(Cell other) {
		return other.x >= 0 && other.y >= 0 && other.x < x && other.y < y;
	}
	Cell plus(Cell other) {
		return new Cell(x + other.x, y + other.y);
	}
	Cell minus(Cell other) {
		return new Cell(x - other.x, y - other.y);
	}
	Cell negate() {
		return new Cell(-x, -y);
	}
	Point toPoint() {
		return new Point(x, y);
	}
	Cell[] lineTo(Cell to) {
		Cell[] result;
		Cell relative = to.minus(this);
		if (Math.abs(relative.x) >= Math.abs(relative.y)) {
			result = new Cell[Math.abs(relative.x) + 1];
			if (relative.x > 0) {
				for (int i = 0; i <= relative.x; ++i)
					result[i] = new Cell(x + i, y + (int)Math.round(i * (relative.y / (double)relative.x)));
			} else if (relative.x < 0) {
				for (int i = 0; i <= -relative.x; ++i)
					result[i] = new Cell(x - i, y - (int)Math.round(i * (relative.y / (double)relative.x)));
			} else
				result[0] = this;
		} else {
			result = new Cell[Math.abs(relative.y) + 1];
			if (relative.y > 0) {
				for (int i = 0; i <= relative.y; ++i)
					result[i] = new Cell(x + (int)Math.round(i * (relative.x / (double)relative.y)), y + i);
			} else if (relative.y < 0) {
				for (int i = 0; i <= -relative.y; ++i)
					result[i] = new Cell(x - (int)Math.round(i * (relative.x / (double)relative.y)), y - i);
			} else
				result[0] = this;
		}
		return result;
	}
	void write(ByteBuffer buffer) {
		buffer.putInt(x);
		buffer.putInt(y);
	}
	static int serializedSize() {
		return 8;
	}
	private List<Object> fields() {
		return Arrays.asList(x, y);
	}
	@Override public boolean equals(Object obj) {
		return obj instanceof Cell && fields().equals(((Cell)obj).fields());
	}
	@Override public int hashCode() {
		return Objects.hash(x, y);
	}
	@Override public String toString() {
		return String.format("[%d,%d]", x, y);
	}
	@Override public Iterator<Cell> iterator() {
		return new CellIterator();
	}
	private class CellIterator implements Iterator<Cell> {
		int atX;
		int atY;
		@Override public boolean hasNext() {
			return atY < y && atX < x;
		}
		@Override public Cell next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Cell result = new Cell(atX, atY);
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
