// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class Histogram {
	public final int width;
	public final int height;
	public final int depth;
	private final int[] array;
	public Histogram(int width, int height, int depth) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		array = new int[width * height * depth];
	}
	public int constrain(int z) {
		return Math.max(0, Math.min(depth - 1, z));
	}
	public int get(int x, int y, int z) {
		return array[offset(x, y, z)];
	}
	public int get(Cell at, int z) {
		return get(at.x, at.y, z);
	}
	public int sum(int x, int y) {
		int sum = 0;
		for (int i = 0; i < depth; ++i)
			sum += get(x, y, i);
		return sum;
	}
	public int sum(Cell at) {
		return sum(at.x, at.y);
	}
	public void set(int x, int y, int z, int value) {
		array[offset(x, y, z)] = value;
	}
	public void set(Cell at, int z, int value) {
		set(at.x, at.y, z, value);
	}
	public void add(int x, int y, int z, int value) {
		array[offset(x, y, z)] += value;
	}
	public void add(Cell at, int z, int value) {
		add(at.x, at.y, z, value);
	}
	public void increment(int x, int y, int z) {
		add(x, y, z, 1);
	}
	public void increment(Cell at, int z) {
		increment(at.x, at.y, z);
	}
	private int offset(int x, int y, int z) {
		return (y * width + x) * depth + z;
	}
}
