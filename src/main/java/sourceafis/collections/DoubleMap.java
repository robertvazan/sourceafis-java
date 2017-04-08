package sourceafis.collections;

public class DoubleMap {
	public final int width;
	public final int height;
	public final double[] array;
	public DoubleMap(int width, int height) {
		this.width = width;
		this.height = height;
		array = new double[width * height];
	}
	public double get(int x, int y) {
		return array[y * width + x];
	}
	public void set(int x, int y, double value) {
		array[y * width + x] = value;
	}
}
