package sourceafis.collections;

import sourceafis.scalars.*;

public class DoubleMap {
	public final int width;
	public final int height;
	public final double[] array;
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
		return array[y * width + x];
	}
	public double get(Cell at) {
		return get(at.x, at.y);
	}
	public void set(int x, int y, double value) {
		array[y * width + x] = value;
	}
	public void set(Cell at, double value) {
		set(at.x, at.y, value);
	}
}
