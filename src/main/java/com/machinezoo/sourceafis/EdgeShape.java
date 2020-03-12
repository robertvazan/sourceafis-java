// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

class EdgeShape {
	private static final int POLAR_CACHE_BITS = 8;
	private static final int POLAR_CACHE_RADIUS = 1 << POLAR_CACHE_BITS;
	private static final int[] POLAR_DISTANCE_CACHE = new int[Integers.sq(POLAR_CACHE_RADIUS)];
	private static final double[] POLAR_ANGLE_CACHE = new double[Integers.sq(POLAR_CACHE_RADIUS)];
	final int length;
	final double referenceAngle;
	final double neighborAngle;
	static {
		for (int y = 0; y < POLAR_CACHE_RADIUS; ++y)
			for (int x = 0; x < POLAR_CACHE_RADIUS; ++x) {
				POLAR_DISTANCE_CACHE[y * POLAR_CACHE_RADIUS + x] = (int)Math.round(Math.sqrt(Integers.sq(x) + Integers.sq(y)));
				if (y > 0 || x > 0)
					POLAR_ANGLE_CACHE[y * POLAR_CACHE_RADIUS + x] = DoubleAngle.atan(new DoublePoint(x, y));
				else
					POLAR_ANGLE_CACHE[y * POLAR_CACHE_RADIUS + x] = 0;
			}
	}
	EdgeShape(int length, double referenceAngle, double neighborAngle) {
		this.length = length;
		this.referenceAngle = referenceAngle;
		this.neighborAngle = neighborAngle;
	}
	EdgeShape(ImmutableMinutia reference, ImmutableMinutia neighbor) {
		IntPoint vector = neighbor.position.minus(reference.position);
		double quadrant = 0;
		int x = vector.x;
		int y = vector.y;
		if (y < 0) {
			x = -x;
			y = -y;
			quadrant = Math.PI;
		}
		if (x < 0) {
			int tmp = -x;
			x = y;
			y = tmp;
			quadrant += DoubleAngle.HALF_PI;
		}
		int shift = 32 - Integer.numberOfLeadingZeros((x | y) >>> POLAR_CACHE_BITS);
		int offset = (y >> shift) * POLAR_CACHE_RADIUS + (x >> shift);
		length = POLAR_DISTANCE_CACHE[offset] << shift;
		double angle = POLAR_ANGLE_CACHE[offset] + quadrant;
		referenceAngle = DoubleAngle.difference(reference.direction, angle);
		neighborAngle = DoubleAngle.difference(neighbor.direction, DoubleAngle.opposite(angle));
	}
}
