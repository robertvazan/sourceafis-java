// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class Histogram {
	final int width;
	final int height;
	final int depth;
	private final int[] array;
	Histogram(int width, int height, int depth) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		array = new int[width * height * depth];
	}
	int constrain(int z) {
		return Math.max(0, Math.min(depth - 1, z));
	}
	int get(int x, int y, int z) {
		return array[offset(x, y, z)];
	}
	int get(Cell at, int z) {
		return get(at.x, at.y, z);
	}
	int sum(int x, int y) {
		int sum = 0;
		for (int i = 0; i < depth; ++i)
			sum += get(x, y, i);
		return sum;
	}
	int sum(Cell at) {
		return sum(at.x, at.y);
	}
	void set(int x, int y, int z, int value) {
		array[offset(x, y, z)] = value;
	}
	void set(Cell at, int z, int value) {
		set(at.x, at.y, z, value);
	}
	void add(int x, int y, int z, int value) {
		array[offset(x, y, z)] += value;
	}
	void add(Cell at, int z, int value) {
		add(at.x, at.y, z, value);
	}
	void increment(int x, int y, int z) {
		add(x, y, z, 1);
	}
	void increment(Cell at, int z) {
		increment(at.x, at.y, z);
	}
	private int offset(int x, int y, int z) {
		return (y * width + x) * depth + z;
	}
}
