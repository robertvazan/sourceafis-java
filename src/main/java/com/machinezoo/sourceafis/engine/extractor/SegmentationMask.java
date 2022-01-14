// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.extractor;

import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class SegmentationMask {
	private static BooleanMatrix filter(BooleanMatrix input) {
		return VoteFilter.vote(input, null, Parameters.BLOCK_ERRORS_VOTE_RADIUS, Parameters.BLOCK_ERRORS_VOTE_MAJORITY, Parameters.BLOCK_ERRORS_VOTE_BORDER_DISTANCE);
	}
	public static BooleanMatrix compute(BlockMap blocks, HistogramCube histogram) {
		DoubleMatrix contrast = ClippedContrast.compute(blocks, histogram);
		BooleanMatrix mask = AbsoluteContrastMask.compute(contrast);
		mask.merge(RelativeContrastMask.compute(contrast, blocks));
		// https://sourceafis.machinezoo.com/transparency/combined-mask
		TransparencySink.current().log("combined-mask", mask);
		mask.merge(filter(mask));
		mask.invert();
		mask.merge(filter(mask));
		mask.merge(filter(mask));
		mask.merge(VoteFilter.vote(mask, null, Parameters.MASK_VOTE_RADIUS, Parameters.MASK_VOTE_MAJORITY, Parameters.MASK_VOTE_BORDER_DISTANCE));
		// https://sourceafis.machinezoo.com/transparency/filtered-mask
		TransparencySink.current().log("filtered-mask", mask);
		return mask;
	}
	public static BooleanMatrix pixelwise(BooleanMatrix mask, BlockMap blocks) {
		BooleanMatrix pixelized = new BooleanMatrix(blocks.pixels);
		for (IntPoint block : blocks.primary.blocks)
			if (mask.get(block))
				for (IntPoint pixel : blocks.primary.block(block))
					pixelized.set(pixel, true);
		// https://sourceafis.machinezoo.com/transparency/pixel-mask
		TransparencySink.current().log("pixel-mask", pixelized);
		return pixelized;
	}
	private static BooleanMatrix shrink(BooleanMatrix mask, int amount) {
		IntPoint size = mask.size();
		BooleanMatrix shrunk = new BooleanMatrix(size);
		for (int y = amount; y < size.y - amount; ++y)
			for (int x = amount; x < size.x - amount; ++x)
				shrunk.set(x, y, mask.get(x, y - amount) && mask.get(x, y + amount) && mask.get(x - amount, y) && mask.get(x + amount, y));
		return shrunk;
	}
	public static BooleanMatrix inner(BooleanMatrix outer) {
		IntPoint size = outer.size();
		BooleanMatrix inner = new BooleanMatrix(size);
		for (int y = 1; y < size.y - 1; ++y)
			for (int x = 1; x < size.x - 1; ++x)
				inner.set(x, y, outer.get(x, y));
		if (Parameters.INNER_MASK_BORDER_DISTANCE >= 1)
			inner = shrink(inner, 1);
		int total = 1;
		for (int step = 1; total + step <= Parameters.INNER_MASK_BORDER_DISTANCE; step *= 2) {
			inner = shrink(inner, step);
			total += step;
		}
		if (total < Parameters.INNER_MASK_BORDER_DISTANCE)
			inner = shrink(inner, Parameters.INNER_MASK_BORDER_DISTANCE - total);
		// https://sourceafis.machinezoo.com/transparency/inner-mask
		TransparencySink.current().log("inner-mask", inner);
		return inner;
	}
}
