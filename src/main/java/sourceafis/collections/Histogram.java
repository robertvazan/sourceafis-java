package sourceafis.collections;

public class Histogram {
	public final int width;
	public final int height;
	public final int depth;
	public final int[] array;
	public Histogram(int width, int height, int depth) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		array = new int[width * height * depth];
	}
	public int get(int x, int y, int z) {
		return array[(y * width + x) * depth + z];
	}
	public void set(int x, int y, int z, int value) {
		array[(y * width + x) * depth + z] = value;
	}
	public void add(int x, int y, int z, int value) {
		array[(y * width + x) * depth + z] += value;
	}
	public void increment(int x, int y, int z) {
		add(x, y, z, 1);
	}
}
