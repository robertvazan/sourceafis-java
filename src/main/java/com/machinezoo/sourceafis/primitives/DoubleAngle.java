// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.primitives;

public class DoubleAngle {
	public static final double PI2 = 2 * Math.PI;
	public static final double INV_PI2 = 1.0 / PI2;
	public static final double HALF_PI = 0.5 * Math.PI;
	public static DoublePoint toVector(double angle) {
		return new DoublePoint(Math.cos(angle), Math.sin(angle));
	}
	public static double atan(DoublePoint vector) {
		double angle = Math.atan2(vector.y, vector.x);
		return angle >= 0 ? angle : angle + PI2;
	}
	public static double atan(IntPoint vector) {
		return atan(vector.toDouble());
	}
	public static double atan(IntPoint center, IntPoint point) {
		return atan(point.minus(center));
	}
	public static double toOrientation(double angle) {
		return angle < Math.PI ? 2 * angle : 2 * (angle - Math.PI);
	}
	public static double fromOrientation(double angle) {
		return 0.5 * angle;
	}
	public static double add(double start, double delta) {
		double angle = start + delta;
		return angle < PI2 ? angle : angle - PI2;
	}
	public static double bucketCenter(int bucket, int resolution) {
		return PI2 * (2 * bucket + 1) / (2 * resolution);
	}
	public static int quantize(double angle, int resolution) {
		int result = (int)(angle * INV_PI2 * resolution);
		if (result < 0)
			return 0;
		else if (result >= resolution)
			return resolution - 1;
		else
			return result;
	}
	public static double opposite(double angle) {
		return angle < Math.PI ? angle + Math.PI : angle - Math.PI;
	}
	public static double distance(double first, double second) {
		double delta = Math.abs(first - second);
		return delta <= Math.PI ? delta : PI2 - delta;
	}
	public static double difference(double first, double second) {
		double angle = first - second;
		return angle >= 0 ? angle : angle + PI2;
	}
	public static double complementary(double angle) {
		double complement = PI2 - angle;
		return complement < PI2 ? complement : complement - PI2;
	}
	public static boolean normalized(double angle) {
		return angle >= 0 && angle < PI2;
	}
}
