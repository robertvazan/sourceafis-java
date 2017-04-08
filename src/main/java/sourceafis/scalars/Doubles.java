package sourceafis.scalars;

public class Doubles {
	public static double sq(double value) {
		return value * value;
	}
	public static double interpolate(double start, double end, double position) {
		return start + position * (end - start);
	}
	public static double interpolate(double topleft, double topright, double bottomleft, double bottomright, FPoint position) {
		double left = interpolate(bottomleft, topleft, position.y);
		double right = interpolate(bottomright, topright, position.y);
		return interpolate(left, right, position.x);
	}
	public static double interpolateExponential(double start, double end, double position) {
		return Math.pow(end / start, position) * start;
	}
}
