package sourceafis.scalars;

public class Angle {
	public static final double PI2 = 2 * Math.PI;
	public static Point toVector(double angle) {
		return new Point(Math.cos(angle), Math.sin(angle));
	}
	public static double atan(Point vector) {
		double angle = Math.atan2(vector.y, vector.x);
		return angle >= 0 ? angle : angle + PI2;
	}
	public static double toOrientation(double angle) {
		return angle < Math.PI ? 2 * angle : 2 * (angle - Math.PI);
	}
	public static double add(double start, double delta) {
		double angle = start + delta;
		return angle < PI2 ? angle : angle - PI2;
	}
}
