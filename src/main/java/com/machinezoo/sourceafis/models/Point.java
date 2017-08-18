// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class Point {
	public static final Point zero = new Point(0, 0);
	public final double x;
	public final double y;
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}
	public Point add(Point other) {
		return new Point(x + other.x, y + other.y);
	}
	public Point negate() {
		return new Point(-x, -y);
	}
	public Point multiply(double factor) {
		return new Point(factor * x, factor * y);
	}
	public Cell round() {
		return new Cell((int)Math.round(x), (int)Math.round(y));
	}
}
