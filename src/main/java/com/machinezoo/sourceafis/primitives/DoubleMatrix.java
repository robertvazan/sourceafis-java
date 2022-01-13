// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.primitives;

public class DoubleMatrix {
	public final int width;
	public final int height;
	private final double[] cells;
	public DoubleMatrix(int width, int height) {
		this.width = width;
		this.height = height;
		cells = new double[width * height];
	}
	public DoubleMatrix(IntPoint size) {
		this(size.x, size.y);
	}
	public IntPoint size() {
		return new IntPoint(width, height);
	}
	public double get(int x, int y) {
		return cells[offset(x, y)];
	}
	public double get(IntPoint at) {
		return get(at.x, at.y);
	}
	public void set(int x, int y, double value) {
		cells[offset(x, y)] = value;
	}
	public void set(IntPoint at, double value) {
		set(at.x, at.y, value);
	}
	public void add(int x, int y, double value) {
		cells[offset(x, y)] += value;
	}
	public void add(IntPoint at, double value) {
		add(at.x, at.y, value);
	}
	public void multiply(int x, int y, double value) {
		cells[offset(x, y)] *= value;
	}
	public void multiply(IntPoint at, double value) {
		multiply(at.x, at.y, value);
	}
	private int offset(int x, int y) {
		return y * width + x;
	}
}
