// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor;

import java.util.*;
import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.primitives.*;
import com.machinezoo.sourceafis.transparency.*;

public class PixelwiseOrientations {
	private static class ConsideredOrientation {
		IntPoint offset;
		DoublePoint orientation;
	}
	private static class OrientationRandom {
		static final int PRIME = 1610612741;
		static final int BITS = 30;
		static final int MASK = (1 << BITS) - 1;
		static final double SCALING = 1.0 / (1 << BITS);
		long state = PRIME * PRIME * PRIME;
		double next() {
			state *= PRIME;
			return ((state & MASK) + 0.5) * SCALING;
		}
	}
	private static ConsideredOrientation[][] plan() {
		OrientationRandom random = new OrientationRandom();
		ConsideredOrientation[][] splits = new ConsideredOrientation[Parameters.ORIENTATION_SPLIT][];
		for (int i = 0; i < Parameters.ORIENTATION_SPLIT; ++i) {
			ConsideredOrientation[] orientations = splits[i] = new ConsideredOrientation[Parameters.ORIENTATIONS_CHECKED];
			for (int j = 0; j < Parameters.ORIENTATIONS_CHECKED; ++j) {
				ConsideredOrientation sample = orientations[j] = new ConsideredOrientation();
				do {
					double angle = random.next() * Math.PI;
					double distance = Doubles.interpolateExponential(Parameters.MIN_ORIENTATION_RADIUS, Parameters.MAX_ORIENTATION_RADIUS, random.next());
					sample.offset = DoubleAngle.toVector(angle).multiply(distance).round();
				} while (sample.offset.equals(IntPoint.ZERO) || sample.offset.y < 0 || Arrays.stream(orientations).limit(j).anyMatch(o -> o.offset.equals(sample.offset)));
				sample.orientation = DoubleAngle.toVector(DoubleAngle.add(DoubleAngle.toOrientation(DoubleAngle.atan(sample.offset.toDouble())), Math.PI));
			}
		}
		return splits;
	}
	private static IntRange maskRange(BooleanMatrix mask, int y) {
		int first = -1;
		int last = -1;
		for (int x = 0; x < mask.width; ++x)
			if (mask.get(x, y)) {
				last = x;
				if (first < 0)
					first = x;
			}
		if (first >= 0)
			return new IntRange(first, last + 1);
		else
			return IntRange.ZERO;
	}
	public static DoublePointMatrix compute(DoubleMatrix input, BooleanMatrix mask, BlockMap blocks) {
		ConsideredOrientation[][] neighbors = plan();
		DoublePointMatrix orientation = new DoublePointMatrix(input.size());
		for (int blockY = 0; blockY < blocks.primary.blocks.y; ++blockY) {
			IntRange maskRange = maskRange(mask, blockY);
			if (maskRange.length() > 0) {
				IntRange validXRange = new IntRange(
					blocks.primary.block(maskRange.start, blockY).left(),
					blocks.primary.block(maskRange.end - 1, blockY).right());
				for (int y = blocks.primary.block(0, blockY).top(); y < blocks.primary.block(0, blockY).bottom(); ++y) {
					for (ConsideredOrientation neighbor : neighbors[y % neighbors.length]) {
						int radius = Math.max(Math.abs(neighbor.offset.x), Math.abs(neighbor.offset.y));
						if (y - radius >= 0 && y + radius < input.height) {
							IntRange xRange = new IntRange(Math.max(radius, validXRange.start), Math.min(input.width - radius, validXRange.end));
							for (int x = xRange.start; x < xRange.end; ++x) {
								double before = input.get(x - neighbor.offset.x, y - neighbor.offset.y);
								double at = input.get(x, y);
								double after = input.get(x + neighbor.offset.x, y + neighbor.offset.y);
								double strength = at - Math.max(before, after);
								if (strength > 0)
									orientation.add(x, y, neighbor.orientation.multiply(strength));
							}
						}
					}
				}
			}
		}
		// https://sourceafis.machinezoo.com/transparency/pixelwise-orientation
		TransparencySink.current().log("pixelwise-orientation", orientation);
		return orientation;
	}
}
