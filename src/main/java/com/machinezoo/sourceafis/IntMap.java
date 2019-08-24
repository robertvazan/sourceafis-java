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
	IntMap(IntPoint size) {
		this(size.x, size.y);
	}
	IntPoint size() {
		return new IntPoint(width, height);
	}
	int get(int x, int y) {
		return array[offset(x, y)];
	}
	int get(IntPoint at) {
		return get(at.x, at.y);
	}
	void set(int x, int y, int value) {
		array[offset(x, y)] = value;
	}
	void set(IntPoint at, int value) {
		set(at.x, at.y, value);
	}
	private int offset(int x, int y) {
		return y * width + x;
	}
}
