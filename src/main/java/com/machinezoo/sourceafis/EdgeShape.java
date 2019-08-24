// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class EdgeShape {
	private static final int polarCacheBits = 8;
	private static final int polarCacheRadius = 1 << polarCacheBits;
	private static final int[] polarDistance = new int[Integers.sq(polarCacheRadius)];
	private static final double[] polarAngle = new double[Integers.sq(polarCacheRadius)];
	final int length;
	final double referenceAngle;
	final double neighborAngle;
	static {
		for (int y = 0; y < polarCacheRadius; ++y)
			for (int x = 0; x < polarCacheRadius; ++x) {
				polarDistance[y * polarCacheRadius + x] = (int)Math.round(Math.sqrt(Integers.sq(x) + Integers.sq(y)));
				if (y > 0 || x > 0)
					polarAngle[y * polarCacheRadius + x] = DoubleAngle.atan(new DoublePoint(x, y));
				else
					polarAngle[y * polarCacheRadius + x] = 0;
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
			quadrant += DoubleAngle.halfPI;
		}
		int shift = 32 - Integer.numberOfLeadingZeros((x | y) >>> polarCacheBits);
		int offset = (y >> shift) * polarCacheRadius + (x >> shift);
		length = polarDistance[offset] << shift;
		double angle = polarAngle[offset] + quadrant;
		referenceAngle = DoubleAngle.difference(reference.direction, angle);
		neighborAngle = DoubleAngle.difference(neighbor.direction, DoubleAngle.opposite(angle));
	}
}
