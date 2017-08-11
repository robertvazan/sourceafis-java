// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import com.machinezoo.sourceafis.scalars.*;

class EdgeShape {
	static final int polarCacheBits = 8;
	static final int polarCacheRadius = 1 << polarCacheBits;
	static final int polarCacheMask = polarCacheRadius - 1;
	static final int[] polarDistance = new int[Integers.sq(polarCacheRadius)];
	static final double[] polarAngle = new double[Integers.sq(polarCacheRadius)];
	static final double HalfPI = 0.5 * Math.PI;
	final int length;
	final double referenceAngle;
	final double neighborAngle;
	static {
		for (int y = 0; y < polarCacheRadius; ++y)
			for (int x = 0; x < polarCacheRadius; ++x) {
				polarDistance[y * polarCacheRadius + x] = (int)Math.round(Math.sqrt(Integers.sq(x) + Integers.sq(y)));
				if (y > 0 || x > 0)
					polarAngle[y * polarCacheRadius + x] = Angle.atan(new Point(x, y));
				else
					polarAngle[y * polarCacheRadius + x] = 0;
			}
	}
	public EdgeShape(FingerprintTemplate template, int reference, int neighbor) {
		FingerprintMinutia referenceMinutia = template.minutiae.get(reference);
		FingerprintMinutia neighborMinutia = template.minutiae.get(neighbor);
		Cell vector = neighborMinutia.position.minus(referenceMinutia.position);
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
			quadrant += HalfPI;
		}
		int shift = 32 - Integer.numberOfLeadingZeros((x | y) >>> polarCacheBits);
		int offset = (y >> shift) * polarCacheRadius + (x >> shift);
		length = polarDistance[offset] << shift;
		double angle = polarAngle[offset] + quadrant;
		referenceAngle = Angle.difference(referenceMinutia.direction, angle);
		neighborAngle = Angle.difference(neighborMinutia.direction, Angle.opposite(angle));
	}
}
