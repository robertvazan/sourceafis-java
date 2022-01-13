// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor;

import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.primitives.*;
import com.machinezoo.sourceafis.transparency.*;

public class BlockOrientations {
	private static DoublePointMatrix aggregate(DoublePointMatrix orientation, BlockMap blocks, BooleanMatrix mask) {
		DoublePointMatrix sums = new DoublePointMatrix(blocks.primary.blocks);
		for (IntPoint block : blocks.primary.blocks) {
			if (mask.get(block)) {
				IntRect area = blocks.primary.block(block);
				for (int y = area.top(); y < area.bottom(); ++y)
					for (int x = area.left(); x < area.right(); ++x)
						sums.add(block, orientation.get(x, y));
			}
		}
		// https://sourceafis.machinezoo.com/transparency/block-orientation
		TransparencySink.current().log("block-orientation", sums);
		return sums;
	}
	private static DoublePointMatrix smooth(DoublePointMatrix orientation, BooleanMatrix mask) {
		IntPoint size = mask.size();
		DoublePointMatrix smoothed = new DoublePointMatrix(size);
		for (IntPoint block : size)
			if (mask.get(block)) {
				IntRect neighbors = IntRect.around(block, Parameters.ORIENTATION_SMOOTHING_RADIUS).intersect(new IntRect(size));
				for (int ny = neighbors.top(); ny < neighbors.bottom(); ++ny)
					for (int nx = neighbors.left(); nx < neighbors.right(); ++nx)
						if (mask.get(nx, ny))
							smoothed.add(block, orientation.get(nx, ny));
			}
		// https://sourceafis.machinezoo.com/transparency/smoothed-orientation
		TransparencySink.current().log("smoothed-orientation", smoothed);
		return smoothed;
	}
	private static DoubleMatrix angles(DoublePointMatrix vectors, BooleanMatrix mask) {
		IntPoint size = mask.size();
		DoubleMatrix angles = new DoubleMatrix(size);
		for (IntPoint block : size)
			if (mask.get(block))
				angles.set(block, DoubleAngle.atan(vectors.get(block)));
		return angles;
	}
	public static DoubleMatrix compute(DoubleMatrix image, BooleanMatrix mask, BlockMap blocks) {
		DoublePointMatrix accumulated = PixelwiseOrientations.compute(image, mask, blocks);
		DoublePointMatrix byBlock = aggregate(accumulated, blocks, mask);
		DoublePointMatrix smooth = smooth(byBlock, mask);
		return angles(smooth, mask);
	}
}
