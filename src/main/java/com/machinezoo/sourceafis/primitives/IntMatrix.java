// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.primitives;

public class IntMatrix {
	public final int width;
	public final int height;
	private final int[] array;
	public IntMatrix(int width, int height) {
		this.width = width;
		this.height = height;
		array = new int[width * height];
	}
	public IntMatrix(IntPoint size) {
		this(size.x, size.y);
	}
	public IntPoint size() {
		return new IntPoint(width, height);
	}
	public int get(int x, int y) {
		return array[offset(x, y)];
	}
	public int get(IntPoint at) {
		return get(at.x, at.y);
	}
	public void set(int x, int y, int value) {
		array[offset(x, y)] = value;
	}
	public void set(IntPoint at, int value) {
		set(at.x, at.y, value);
	}
	private int offset(int x, int y) {
		return y * width + x;
	}
}
