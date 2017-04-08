package sourceafis.collections;

import sourceafis.scalars.*;

public class BooleanMap {
	public final int width;
	public final int height;
	public final boolean[] array;
	public BooleanMap(int width, int height) {
		this.width = width;
		this.height = height;
		array = new boolean[width * height];
	}
	public BooleanMap(Cell size) {
		this(size.x, size.y);
	}
	public boolean get(int x, int y) {
		return array[y * width + x];
	}
	public boolean get(Cell at) {
		return get(at.x, at.y);
	}
	public void set(int x, int y, boolean value) {
		array[y * width + x] = value;
	}
	public void set(Cell at, boolean value) {
		set(at.x, at.y, value);
	}
}
