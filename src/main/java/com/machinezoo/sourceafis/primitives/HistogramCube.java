// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.primitives;

public class HistogramCube {
	public final int width;
	public final int height;
	public final int bins;
	private final int[] counts;
	public HistogramCube(int width, int height, int bins) {
		this.width = width;
		this.height = height;
		this.bins = bins;
		counts = new int[width * height * bins];
	}
	public HistogramCube(IntPoint size, int bins) {
		this(size.x, size.y, bins);
	}
	public int constrain(int z) {
		return Math.max(0, Math.min(bins - 1, z));
	}
	public int get(int x, int y, int z) {
		return counts[offset(x, y, z)];
	}
	public int get(IntPoint at, int z) {
		return get(at.x, at.y, z);
	}
	public int sum(int x, int y) {
		int sum = 0;
		for (int i = 0; i < bins; ++i)
			sum += get(x, y, i);
		return sum;
	}
	public int sum(IntPoint at) {
		return sum(at.x, at.y);
	}
	public void set(int x, int y, int z, int value) {
		counts[offset(x, y, z)] = value;
	}
	public void set(IntPoint at, int z, int value) {
		set(at.x, at.y, z, value);
	}
	public void add(int x, int y, int z, int value) {
		counts[offset(x, y, z)] += value;
	}
	public void add(IntPoint at, int z, int value) {
		add(at.x, at.y, z, value);
	}
	public void increment(int x, int y, int z) {
		add(x, y, z, 1);
	}
	public void increment(IntPoint at, int z) {
		increment(at.x, at.y, z);
	}
	private int offset(int x, int y, int z) {
		return (y * width + x) * bins + z;
	}
}
