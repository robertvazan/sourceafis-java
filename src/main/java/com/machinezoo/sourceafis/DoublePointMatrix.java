// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

class DoublePointMatrix {
	final int width;
	final int height;
	private final double[] vectors;
	DoublePointMatrix(int width, int height) {
		this.width = width;
		this.height = height;
		vectors = new double[2 * width * height];
	}
	DoublePointMatrix(IntPoint size) {
		this(size.x, size.y);
	}
	IntPoint size() {
		return new IntPoint(width, height);
	}
	DoublePoint get(int x, int y) {
		int i = offset(x, y);
		return new DoublePoint(vectors[i], vectors[i + 1]);
	}
	DoublePoint get(IntPoint at) {
		return get(at.x, at.y);
	}
	void set(int x, int y, double px, double py) {
		int i = offset(x, y);
		vectors[i] = px;
		vectors[i + 1] = py;
	}
	void set(int x, int y, DoublePoint point) {
		set(x, y, point.x, point.y);
	}
	void set(IntPoint at, DoublePoint point) {
		set(at.x, at.y, point);
	}
	void add(int x, int y, double px, double py) {
		int i = offset(x, y);
		vectors[i] += px;
		vectors[i + 1] += py;
	}
	void add(int x, int y, DoublePoint point) {
		add(x, y, point.x, point.y);
	}
	void add(IntPoint at, DoublePoint point) {
		add(at.x, at.y, point);
	}
	private int offset(int x, int y) {
		return 2 * (y * width + x);
	}
}
