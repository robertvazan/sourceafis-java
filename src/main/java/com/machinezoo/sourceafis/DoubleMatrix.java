// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.nio.*;

class DoubleMatrix {
	final int width;
	final int height;
	private final double[] array;
	DoubleMatrix(int width, int height) {
		this.width = width;
		this.height = height;
		array = new double[width * height];
	}
	DoubleMatrix(IntPoint size) {
		this(size.x, size.y);
	}
	IntPoint size() {
		return new IntPoint(width, height);
	}
	double get(int x, int y) {
		return array[offset(x, y)];
	}
	double get(IntPoint at) {
		return get(at.x, at.y);
	}
	void set(int x, int y, double value) {
		array[offset(x, y)] = value;
	}
	void set(IntPoint at, double value) {
		set(at.x, at.y, value);
	}
	void add(int x, int y, double value) {
		array[offset(x, y)] += value;
	}
	void add(IntPoint at, double value) {
		add(at.x, at.y, value);
	}
	void multiply(int x, int y, double value) {
		array[offset(x, y)] *= value;
	}
	void multiply(IntPoint at, double value) {
		multiply(at.x, at.y, value);
	}
	byte[] serialize() {
		ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES * array.length);
		buffer.asDoubleBuffer().put(array);
		return buffer.array();
	}
	JsonArrayInfo json() {
		JsonArrayInfo info = new JsonArrayInfo();
		info.axes = new String[] { "y", "x" };
		info.dimensions = new int[] { height, width };
		info.scalar = "double";
		info.bitness = 64;
		info.endianness = "big";
		info.format = "IEEE754";
		return info;
	}
	private int offset(int x, int y) {
		return y * width + x;
	}
}
