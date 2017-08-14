// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class Integers {
	public static int sq(int value) {
		return value * value;
	}
	public static int roundUpDiv(int dividend, int divisor) {
		return (dividend + divisor - 1) / divisor;
	}
}
