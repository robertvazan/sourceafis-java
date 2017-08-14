// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class BooleanMap {
	public final int width;
	public final int height;
	private final boolean[] array;
	public BooleanMap(int width, int height) {
		this.width = width;
		this.height = height;
		array = new boolean[width * height];
	}
	public BooleanMap(Cell size) {
		this(size.x, size.y);
	}
	public BooleanMap(BooleanMap other) {
		this(other.size());
		for (int i = 0; i < array.length; ++i)
			array[i] = other.array[i];
	}
	public Cell size() {
		return new Cell(width, height);
	}
	public boolean get(int x, int y) {
		return array[offset(x, y)];
	}
	public boolean get(Cell at) {
		return get(at.x, at.y);
	}
	public boolean get(int x, int y, boolean fallback) {
		if (x < 0 || y < 0 || x >= width || y >= height)
			return fallback;
		return array[offset(x, y)];
	}
	public boolean get(Cell at, boolean fallback) {
		return get(at.x, at.y, fallback);
	}
	public void set(int x, int y, boolean value) {
		array[offset(x, y)] = value;
	}
	public void set(Cell at, boolean value) {
		set(at.x, at.y, value);
	}
	public void invert() {
		for (int i = 0; i < array.length; ++i)
			array[i] = !array[i];
	}
	public void merge(BooleanMap other) {
		if (other.width != width || other.height != height)
			throw new IllegalArgumentException();
		for (int i = 0; i < array.length; ++i)
			array[i] |= other.array[i];
	}
	private int offset(int x, int y) {
		return y * width + x;
	}
}
