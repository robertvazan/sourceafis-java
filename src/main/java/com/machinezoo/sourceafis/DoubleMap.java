// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class DoubleMap {
	final int width;
	final int height;
	private final double[] array;
	DoubleMap(int width, int height) {
		this.width = width;
		this.height = height;
		array = new double[width * height];
	}
	DoubleMap(Cell size) {
		this(size.x, size.y);
	}
	Cell size() {
		return new Cell(width, height);
	}
	double get(int x, int y) {
		return array[offset(x, y)];
	}
	double get(Cell at) {
		return get(at.x, at.y);
	}
	void set(int x, int y, double value) {
		array[offset(x, y)] = value;
	}
	void set(Cell at, double value) {
		set(at.x, at.y, value);
	}
	void add(int x, int y, double value) {
		array[offset(x, y)] += value;
	}
	void add(Cell at, double value) {
		add(at.x, at.y, value);
	}
	void multiply(int x, int y, double value) {
		array[offset(x, y)] *= value;
	}
	void multiply(Cell at, double value) {
		multiply(at.x, at.y, value);
	}
	private int offset(int x, int y) {
		return y * width + x;
	}
}
