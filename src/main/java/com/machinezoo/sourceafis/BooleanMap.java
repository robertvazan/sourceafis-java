// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.nio.*;

class BooleanMap {
	final int width;
	final int height;
	private final boolean[] array;
	BooleanMap(int width, int height) {
		this.width = width;
		this.height = height;
		array = new boolean[width * height];
	}
	BooleanMap(Cell size) {
		this(size.x, size.y);
	}
	BooleanMap(BooleanMap other) {
		this(other.size());
		for (int i = 0; i < array.length; ++i)
			array[i] = other.array[i];
	}
	Cell size() {
		return new Cell(width, height);
	}
	boolean get(int x, int y) {
		return array[offset(x, y)];
	}
	boolean get(Cell at) {
		return get(at.x, at.y);
	}
	boolean get(int x, int y, boolean fallback) {
		if (x < 0 || y < 0 || x >= width || y >= height)
			return fallback;
		return array[offset(x, y)];
	}
	boolean get(Cell at, boolean fallback) {
		return get(at.x, at.y, fallback);
	}
	void set(int x, int y, boolean value) {
		array[offset(x, y)] = value;
	}
	void set(Cell at, boolean value) {
		set(at.x, at.y, value);
	}
	void invert() {
		for (int i = 0; i < array.length; ++i)
			array[i] = !array[i];
	}
	void merge(BooleanMap other) {
		if (other.width != width || other.height != height)
			throw new IllegalArgumentException();
		for (int i = 0; i < array.length; ++i)
			array[i] |= other.array[i];
	}
	ByteBuffer serialize() {
		ByteBuffer buffer = ByteBuffer.allocate(size().area());
		for (Cell at : size())
			buffer.put((byte)(get(at) ? 1 : 0));
		buffer.flip();
		return buffer;
	}
	JsonArrayInfo json() {
		JsonArrayInfo info = new JsonArrayInfo();
		info.axes = new String[] { "y", "x" };
		info.dimensions = new int[] { height, width };
		info.scalar = "boolean";
		info.bitness = 8;
		info.format = "false as 0, true as 1";
		return info;
	}
	private int offset(int x, int y) {
		return y * width + x;
	}
}
