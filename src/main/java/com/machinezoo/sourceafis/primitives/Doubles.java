// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.primitives;

public class Doubles {
	public static double sq(double value) {
		return value * value;
	}
	public static double interpolate(double start, double end, double position) {
		return start + position * (end - start);
	}
	public static double interpolate(double bottomleft, double bottomright, double topleft, double topright, double x, double y) {
		double left = interpolate(topleft, bottomleft, y);
		double right = interpolate(topright, bottomright, y);
		return interpolate(left, right, x);
	}
	public static double interpolateExponential(double start, double end, double position) {
		return Math.pow(end / start, position) * start;
	}
}
