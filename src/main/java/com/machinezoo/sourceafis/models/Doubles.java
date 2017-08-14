// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class Doubles {
	public static double sq(double value) {
		return value * value;
	}
	public static double interpolate(double start, double end, double position) {
		return start + position * (end - start);
	}
	public static double interpolate(double topleft, double topright, double bottomleft, double bottomright, double x, double y) {
		double left = interpolate(bottomleft, topleft, y);
		double right = interpolate(bottomright, topright, y);
		return interpolate(left, right, x);
	}
	public static double interpolateExponential(double start, double end, double position) {
		return Math.pow(end / start, position) * start;
	}
}
