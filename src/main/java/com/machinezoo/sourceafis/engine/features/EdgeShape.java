// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.features;

import com.machinezoo.sourceafis.engine.primitives.*;

public class EdgeShape {
	private static final int POLAR_CACHE_BITS = 8;
	private static final int POLAR_CACHE_RADIUS = 1 << POLAR_CACHE_BITS;
	private static final int[] POLAR_DISTANCE_CACHE = new int[Integers.sq(POLAR_CACHE_RADIUS)];
	private static final float[] POLAR_ANGLE_CACHE = new float[Integers.sq(POLAR_CACHE_RADIUS)];
	public final short length;
	public final float referenceAngle;
	public final float neighborAngle;
	static {
		for (int y = 0; y < POLAR_CACHE_RADIUS; ++y)
			for (int x = 0; x < POLAR_CACHE_RADIUS; ++x) {
				POLAR_DISTANCE_CACHE[y * POLAR_CACHE_RADIUS + x] = (int)Math.round(Math.sqrt(Integers.sq(x) + Integers.sq(y)));
				if (y > 0 || x > 0)
					POLAR_ANGLE_CACHE[y * POLAR_CACHE_RADIUS + x] = (float)DoubleAngle.atan(new DoublePoint(x, y));
				else
					POLAR_ANGLE_CACHE[y * POLAR_CACHE_RADIUS + x] = 0;
			}
	}
	public EdgeShape(short length, float referenceAngle, float neighborAngle) {
		this.length = length;
		this.referenceAngle = referenceAngle;
		this.neighborAngle = neighborAngle;
	}
	public EdgeShape(SearchMinutia reference, SearchMinutia neighbor) {
		float quadrant = 0;
		int x = neighbor.x - reference.x;
		int y = neighbor.y - reference.y;
		if (y < 0) {
			x = -x;
			y = -y;
			quadrant = FloatAngle.PI;
		}
		if (x < 0) {
			int tmp = -x;
			x = y;
			y = tmp;
			quadrant += FloatAngle.HALF_PI;
		}
		int shift = 32 - Integer.numberOfLeadingZeros((x | y) >>> POLAR_CACHE_BITS);
		int offset = (y >> shift) * POLAR_CACHE_RADIUS + (x >> shift);
		length = (short)(POLAR_DISTANCE_CACHE[offset] << shift);
		float angle = POLAR_ANGLE_CACHE[offset] + quadrant;
		referenceAngle = FloatAngle.difference(reference.direction, angle);
		neighborAngle = FloatAngle.difference(neighbor.direction, FloatAngle.opposite(angle));
	}
}
