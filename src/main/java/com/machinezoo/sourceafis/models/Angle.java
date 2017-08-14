// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class Angle {
	public static final double PI2 = 2 * Math.PI;
	static final double invPI2 = 1.0 / PI2;
	public static Point toVector(double angle) {
		return new Point(Math.cos(angle), Math.sin(angle));
	}
	public static double atan(Point vector) {
		double angle = Math.atan2(vector.y, vector.x);
		return angle >= 0 ? angle : angle + PI2;
	}
	public static double atan(Cell vector) {
		return atan(vector.toPoint());
	}
	public static double atan(Cell center, Cell point) {
		return atan(point.minus(center));
	}
	public static double toOrientation(double angle) {
		return angle < Math.PI ? 2 * angle : 2 * (angle - Math.PI);
	}
	public static double add(double start, double delta) {
		double angle = start + delta;
		return angle < PI2 ? angle : angle - PI2;
	}
	public static double bucketCenter(int bucket, int resolution) {
		return PI2 * (2 * bucket + 1) / (2 * resolution);
	}
	public static int quantize(double angle, int resolution) {
		int result = (int)(angle * invPI2 * resolution);
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
}
