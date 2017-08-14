// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class DoubleMap {
	public final int width;
	public final int height;
	private final double[] array;
	public DoubleMap(int width, int height) {
		this.width = width;
		this.height = height;
		array = new double[width * height];
	}
	public DoubleMap(Cell size) {
		this(size.x, size.y);
	}
	public Cell size() {
		return new Cell(width, height);
	}
	public double get(int x, int y) {
		return array[offset(x, y)];
	}
	public double get(Cell at) {
		return get(at.x, at.y);
	}
	public void set(int x, int y, double value) {
		array[offset(x, y)] = value;
	}
	public void set(Cell at, double value) {
		set(at.x, at.y, value);
	}
	public void add(int x, int y, double value) {
		array[offset(x, y)] += value;
	}
	public void add(Cell at, double value) {
		add(at.x, at.y, value);
	}
	public void multiply(int x, int y, double value) {
		array[offset(x, y)] *= value;
	}
	public void multiply(Cell at, double value) {
		multiply(at.x, at.y, value);
	}
	private int offset(int x, int y) {
		return y * width + x;
	}
}
