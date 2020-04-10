// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

class BooleanMatrix {
	final int width;
	final int height;
	private final boolean[] cells;
	BooleanMatrix(int width, int height) {
		this.width = width;
		this.height = height;
		cells = new boolean[width * height];
	}
	BooleanMatrix(IntPoint size) {
		this(size.x, size.y);
	}
	BooleanMatrix(BooleanMatrix other) {
		this(other.size());
		for (int i = 0; i < cells.length; ++i)
			cells[i] = other.cells[i];
	}
	IntPoint size() {
		return new IntPoint(width, height);
	}
	boolean get(int x, int y) {
		return cells[offset(x, y)];
	}
	boolean get(IntPoint at) {
		return get(at.x, at.y);
	}
	boolean get(int x, int y, boolean fallback) {
		if (x < 0 || y < 0 || x >= width || y >= height)
			return fallback;
		return cells[offset(x, y)];
	}
	boolean get(IntPoint at, boolean fallback) {
		return get(at.x, at.y, fallback);
	}
	void set(int x, int y, boolean value) {
		cells[offset(x, y)] = value;
	}
	void set(IntPoint at, boolean value) {
		set(at.x, at.y, value);
	}
	void invert() {
		for (int i = 0; i < cells.length; ++i)
			cells[i] = !cells[i];
	}
	void merge(BooleanMatrix other) {
		if (other.width != width || other.height != height)
			throw new IllegalArgumentException();
		for (int i = 0; i < cells.length; ++i)
			cells[i] |= other.cells[i];
	}
	private int offset(int x, int y) {
		return y * width + x;
	}
}
