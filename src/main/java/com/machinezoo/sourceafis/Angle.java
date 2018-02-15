// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class Angle {
	static final double PI2 = 2 * Math.PI;
	static final double invPI2 = 1.0 / PI2;
	static final double halfPI = 0.5 * Math.PI;
	static Point toVector(double angle) {
		return new Point(Math.cos(angle), Math.sin(angle));
	}
	static double atan(Point vector) {
		double angle = Math.atan2(vector.y, vector.x);
		return angle >= 0 ? angle : angle + PI2;
	}
	static double atan(Cell vector) {
		return atan(vector.toPoint());
	}
	static double atan(Cell center, Cell point) {
		return atan(point.minus(center));
	}
	static double toOrientation(double angle) {
		return angle < Math.PI ? 2 * angle : 2 * (angle - Math.PI);
	}
	static double fromOrientation(double angle) {
		return 0.5 * angle;
	}
	static double add(double start, double delta) {
		double angle = start + delta;
		return angle < PI2 ? angle : angle - PI2;
	}
	static double bucketCenter(int bucket, int resolution) {
		return PI2 * (2 * bucket + 1) / (2 * resolution);
	}
	static int quantize(double angle, int resolution) {
		int result = (int)(angle * invPI2 * resolution);
		if (result < 0)
			return 0;
		else if (result >= resolution)
			return resolution - 1;
		else
			return result;
	}
	static double opposite(double angle) {
		return angle < Math.PI ? angle + Math.PI : angle - Math.PI;
	}
	static double distance(double first, double second) {
		double delta = Math.abs(first - second);
		return delta <= Math.PI ? delta : PI2 - delta;
	}
	static double difference(double first, double second) {
		double angle = first - second;
		return angle >= 0 ? angle : angle + PI2;
	}
	static double complementary(double angle) {
		double complement = PI2 - angle;
		return complement < PI2 ? complement : complement - PI2;
	}
}
