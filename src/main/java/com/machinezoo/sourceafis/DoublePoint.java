// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class DoublePoint {
	static final DoublePoint zero = new DoublePoint(0, 0);
	final double x;
	final double y;
	DoublePoint(double x, double y) {
		this.x = x;
		this.y = y;
	}
	DoublePoint add(DoublePoint other) {
		return new DoublePoint(x + other.x, y + other.y);
	}
	DoublePoint negate() {
		return new DoublePoint(-x, -y);
	}
	DoublePoint multiply(double factor) {
		return new DoublePoint(factor * x, factor * y);
	}
	IntPoint round() {
		return new IntPoint((int)Math.round(x), (int)Math.round(y));
	}
}
