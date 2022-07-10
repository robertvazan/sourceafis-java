// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.primitives;

public class FloatAngle {
	public static final float PI2 = (float)DoubleAngle.PI2;
	public static final float PI = (float)Math.PI;
	public static final float HALF_PI = (float)DoubleAngle.HALF_PI;
	public static float add(float start, float delta) {
		float angle = start + delta;
		return angle < PI2 ? angle : angle - PI2;
	}
	public static float difference(float first, float second) {
		float angle = first - second;
		return angle >= 0 ? angle : angle + PI2;
	}
	public static float distance(float first, float second) {
		float delta = Math.abs(first - second);
		return delta <= PI ? delta : PI2 - delta;
	}
	public static float opposite(float angle) {
		return angle < PI ? angle + PI : angle - PI;
	}
	public static float complementary(float angle) {
		float complement = PI2 - angle;
		return complement < PI2 ? complement : complement - PI2;
	}
	public static boolean normalized(float angle) {
		return angle >= 0 && angle < PI2;
	}
}
