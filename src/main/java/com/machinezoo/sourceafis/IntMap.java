// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class IntMap {
	final int width;
	final int height;
	private final int[] array;
	IntMap(int width, int height) {
		this.width = width;
		this.height = height;
		array = new int[width * height];
	}
	IntMap(Cell size) {
		this(size.x, size.y);
	}
	Cell size() {
		return new Cell(width, height);
	}
	int get(int x, int y) {
		return array[offset(x, y)];
	}
	int get(Cell at) {
		return get(at.x, at.y);
	}
	void set(int x, int y, int value) {
		array[offset(x, y)] = value;
	}
	void set(Cell at, int value) {
		set(at.x, at.y, value);
	}
	private int offset(int x, int y) {
		return y * width + x;
	}
}
