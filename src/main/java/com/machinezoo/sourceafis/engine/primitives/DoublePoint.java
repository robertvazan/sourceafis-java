// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.primitives;

public class DoublePoint {
	public static final DoublePoint ZERO = new DoublePoint(0, 0);
	public final double x;
	public final double y;
	public DoublePoint(double x, double y) {
		this.x = x;
		this.y = y;
	}
	public DoublePoint add(DoublePoint other) {
		return new DoublePoint(x + other.x, y + other.y);
	}
	public DoublePoint negate() {
		return new DoublePoint(-x, -y);
	}
	public DoublePoint multiply(double factor) {
		return new DoublePoint(factor * x, factor * y);
	}
	public IntPoint round() {
		return new IntPoint((int)Math.round(x), (int)Math.round(y));
	}
}
