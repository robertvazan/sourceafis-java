package sourceafis.scalars;

public class FPoint {
	public final double x;
	public final double y;
	public FPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}
	public FPoint(Point point) {
		this(point.x, point.y);
	}
	public FPoint add(FPoint other) {
		return new FPoint(x + other.x, y + other.y);
	}
	public FPoint multiply(double factor) {
		return new FPoint(factor * x, factor * y);
	}
}
