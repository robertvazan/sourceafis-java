// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class Point {
	static final Point zero = new Point(0, 0);
	final double x;
	final double y;
	Point(double x, double y) {
		this.x = x;
		this.y = y;
	}
	Point add(Point other) {
		return new Point(x + other.x, y + other.y);
	}
	Point negate() {
		return new Point(-x, -y);
	}
	Point multiply(double factor) {
		return new Point(factor * x, factor * y);
	}
	Cell round() {
		return new Cell((int)Math.round(x), (int)Math.round(y));
	}
}
