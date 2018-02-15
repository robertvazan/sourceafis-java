// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class Doubles {
	static double sq(double value) {
		return value * value;
	}
	static double interpolate(double start, double end, double position) {
		return start + position * (end - start);
	}
	static double interpolate(double bottomleft, double bottomright, double topleft, double topright, double x, double y) {
		double left = interpolate(topleft, bottomleft, y);
		double right = interpolate(topright, bottomright, y);
		return interpolate(left, right, x);
	}
	static double interpolateExponential(double start, double end, double position) {
		return Math.pow(end / start, position) * start;
	}
}
