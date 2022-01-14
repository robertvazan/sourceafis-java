// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.primitives;

public class DoublePointMatrix {
	public final int width;
	public final int height;
	private final double[] vectors;
	public DoublePointMatrix(int width, int height) {
		this.width = width;
		this.height = height;
		vectors = new double[2 * width * height];
	}
	public DoublePointMatrix(IntPoint size) {
		this(size.x, size.y);
	}
	public IntPoint size() {
		return new IntPoint(width, height);
	}
	public DoublePoint get(int x, int y) {
		int i = offset(x, y);
		return new DoublePoint(vectors[i], vectors[i + 1]);
	}
	public DoublePoint get(IntPoint at) {
		return get(at.x, at.y);
	}
	public void set(int x, int y, double px, double py) {
		int i = offset(x, y);
		vectors[i] = px;
		vectors[i + 1] = py;
	}
	public void set(int x, int y, DoublePoint point) {
		set(x, y, point.x, point.y);
	}
	public void set(IntPoint at, DoublePoint point) {
		set(at.x, at.y, point);
	}
	public void add(int x, int y, double px, double py) {
		int i = offset(x, y);
		vectors[i] += px;
		vectors[i + 1] += py;
	}
	public void add(int x, int y, DoublePoint point) {
		add(x, y, point.x, point.y);
	}
	public void add(IntPoint at, DoublePoint point) {
		add(at.x, at.y, point);
	}
	private int offset(int x, int y) {
		return 2 * (y * width + x);
	}
}
