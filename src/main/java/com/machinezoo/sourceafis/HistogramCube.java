// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

class HistogramCube {
	final int width;
	final int height;
	final int bins;
	private final int[] counts;
	HistogramCube(int width, int height, int bins) {
		this.width = width;
		this.height = height;
		this.bins = bins;
		counts = new int[width * height * bins];
	}
	HistogramCube(IntPoint size, int bins) {
		this(size.x, size.y, bins);
	}
	int constrain(int z) {
		return Math.max(0, Math.min(bins - 1, z));
	}
	int get(int x, int y, int z) {
		return counts[offset(x, y, z)];
	}
	int get(IntPoint at, int z) {
		return get(at.x, at.y, z);
	}
	int sum(int x, int y) {
		int sum = 0;
		for (int i = 0; i < bins; ++i)
			sum += get(x, y, i);
		return sum;
	}
	int sum(IntPoint at) {
		return sum(at.x, at.y);
	}
	void set(int x, int y, int z, int value) {
		counts[offset(x, y, z)] = value;
	}
	void set(IntPoint at, int z, int value) {
		set(at.x, at.y, z, value);
	}
	void add(int x, int y, int z, int value) {
		counts[offset(x, y, z)] += value;
	}
	void add(IntPoint at, int z, int value) {
		add(at.x, at.y, z, value);
	}
	void increment(int x, int y, int z) {
		add(x, y, z, 1);
	}
	void increment(IntPoint at, int z) {
		increment(at.x, at.y, z);
	}
	private int offset(int x, int y, int z) {
		return (y * width + x) * bins + z;
	}
}
