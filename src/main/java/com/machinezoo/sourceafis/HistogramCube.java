// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.nio.*;

class HistogramCube {
	final int width;
	final int height;
	final int depth;
	private final int[] array;
	HistogramCube(int width, int height, int depth) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		array = new int[width * height * depth];
	}
	HistogramCube(IntPoint size, int depth) {
		this(size.x, size.y, depth);
	}
	int constrain(int z) {
		return Math.max(0, Math.min(depth - 1, z));
	}
	int get(int x, int y, int z) {
		return array[offset(x, y, z)];
	}
	int get(IntPoint at, int z) {
		return get(at.x, at.y, z);
	}
	int sum(int x, int y) {
		int sum = 0;
		for (int i = 0; i < depth; ++i)
			sum += get(x, y, i);
		return sum;
	}
	int sum(IntPoint at) {
		return sum(at.x, at.y);
	}
	void set(int x, int y, int z, int value) {
		array[offset(x, y, z)] = value;
	}
	void set(IntPoint at, int z, int value) {
		set(at.x, at.y, z, value);
	}
	void add(int x, int y, int z, int value) {
		array[offset(x, y, z)] += value;
	}
	void add(IntPoint at, int z, int value) {
		add(at.x, at.y, z, value);
	}
	void increment(int x, int y, int z) {
		add(x, y, z, 1);
	}
	void increment(IntPoint at, int z) {
		increment(at.x, at.y, z);
	}
	byte[] serialize() {
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * array.length);
		buffer.asIntBuffer().put(array);
		return buffer.array();
	}
	JsonArrayInfo json() {
		JsonArrayInfo info = new JsonArrayInfo();
		info.axes = new String[] { "y", "x", "bin" };
		info.dimensions = new int[] { height, width, depth };
		info.scalar = "int";
		info.bitness = 32;
		info.endianness = "big";
		info.format = "signed";
		return info;
	}
	private int offset(int x, int y, int z) {
		return (y * width + x) * depth + z;
	}
}
