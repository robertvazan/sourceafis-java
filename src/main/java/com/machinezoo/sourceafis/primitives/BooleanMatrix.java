// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.primitives;

public class BooleanMatrix {
	public final int width;
	public final int height;
	private final boolean[] cells;
	public BooleanMatrix(int width, int height) {
		this.width = width;
		this.height = height;
		cells = new boolean[width * height];
	}
	public BooleanMatrix(IntPoint size) {
		this(size.x, size.y);
	}
	public BooleanMatrix(BooleanMatrix other) {
		this(other.size());
		for (int i = 0; i < cells.length; ++i)
			cells[i] = other.cells[i];
	}
	public IntPoint size() {
		return new IntPoint(width, height);
	}
	public boolean get(int x, int y) {
		return cells[offset(x, y)];
	}
	public boolean get(IntPoint at) {
		return get(at.x, at.y);
	}
	public boolean get(int x, int y, boolean fallback) {
		if (x < 0 || y < 0 || x >= width || y >= height)
			return fallback;
		return cells[offset(x, y)];
	}
	public boolean get(IntPoint at, boolean fallback) {
		return get(at.x, at.y, fallback);
	}
	public void set(int x, int y, boolean value) {
		cells[offset(x, y)] = value;
	}
	public void set(IntPoint at, boolean value) {
		set(at.x, at.y, value);
	}
	public void invert() {
		for (int i = 0; i < cells.length; ++i)
			cells[i] = !cells[i];
	}
	public void merge(BooleanMatrix other) {
		if (other.width != width || other.height != height)
			throw new IllegalArgumentException();
		for (int i = 0; i < cells.length; ++i)
			cells[i] |= other.cells[i];
	}
	private int offset(int x, int y) {
		return y * width + x;
	}
}
