// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

class DoubleMatrix {
	final int width;
	final int height;
	private final double[] cells;
	DoubleMatrix(int width, int height) {
		this.width = width;
		this.height = height;
		cells = new double[width * height];
	}
	DoubleMatrix(IntPoint size) {
		this(size.x, size.y);
	}
	IntPoint size() {
		return new IntPoint(width, height);
	}
	double get(int x, int y) {
		return cells[offset(x, y)];
	}
	double get(IntPoint at) {
		return get(at.x, at.y);
	}
	void set(int x, int y, double value) {
		cells[offset(x, y)] = value;
	}
	void set(IntPoint at, double value) {
		set(at.x, at.y, value);
	}
	void add(int x, int y, double value) {
		cells[offset(x, y)] += value;
	}
	void add(IntPoint at, double value) {
		add(at.x, at.y, value);
	}
	void multiply(int x, int y, double value) {
		cells[offset(x, y)] *= value;
	}
	void multiply(IntPoint at, double value) {
		multiply(at.x, at.y, value);
	}
	private int offset(int x, int y) {
		return y * width + x;
	}
}
