// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.extractor;

import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class BinarizedImage {
	public static BooleanMatrix binarize(DoubleMatrix input, DoubleMatrix baseline, BooleanMatrix mask, BlockMap blocks) {
		IntPoint size = input.size();
		BooleanMatrix binarized = new BooleanMatrix(size);
		for (IntPoint block : blocks.primary.blocks)
			if (mask.get(block)) {
				IntRect rect = blocks.primary.block(block);
				for (int y = rect.top(); y < rect.bottom(); ++y)
					for (int x = rect.left(); x < rect.right(); ++x)
						if (input.get(x, y) - baseline.get(x, y) > 0)
							binarized.set(x, y, true);
			}
		// https://sourceafis.machinezoo.com/transparency/binarized-image
		TransparencySink.current().log("binarized-image", binarized);
		return binarized;
	}
	private static void removeCrosses(BooleanMatrix input) {
		IntPoint size = input.size();
		boolean any = true;
		while (any) {
			any = false;
			for (int y = 0; y < size.y - 1; ++y)
				for (int x = 0; x < size.x - 1; ++x)
					if (input.get(x, y) && input.get(x + 1, y + 1) && !input.get(x, y + 1) && !input.get(x + 1, y)
						|| input.get(x, y + 1) && input.get(x + 1, y) && !input.get(x, y) && !input.get(x + 1, y + 1)) {
						input.set(x, y, false);
						input.set(x, y + 1, false);
						input.set(x + 1, y, false);
						input.set(x + 1, y + 1, false);
						any = true;
					}
		}
	}
	public static void cleanup(BooleanMatrix binary, BooleanMatrix mask) {
		IntPoint size = binary.size();
		BooleanMatrix inverted = new BooleanMatrix(binary);
		inverted.invert();
		BooleanMatrix islands = VoteFilter.vote(inverted, mask, Parameters.BINARIZED_VOTE_RADIUS, Parameters.BINARIZED_VOTE_MAJORITY, Parameters.BINARIZED_VOTE_BORDER_DISTANCE);
		BooleanMatrix holes = VoteFilter.vote(binary, mask, Parameters.BINARIZED_VOTE_RADIUS, Parameters.BINARIZED_VOTE_MAJORITY, Parameters.BINARIZED_VOTE_BORDER_DISTANCE);
		for (int y = 0; y < size.y; ++y)
			for (int x = 0; x < size.x; ++x)
				binary.set(x, y, binary.get(x, y) && !islands.get(x, y) || holes.get(x, y));
		removeCrosses(binary);
		// https://sourceafis.machinezoo.com/transparency/filtered-binary-image
		TransparencySink.current().log("filtered-binary-image", binary);
	}
	public static BooleanMatrix invert(BooleanMatrix binary, BooleanMatrix mask) {
		IntPoint size = binary.size();
		BooleanMatrix inverted = new BooleanMatrix(size);
		for (int y = 0; y < size.y; ++y)
			for (int x = 0; x < size.x; ++x)
				inverted.set(x, y, !binary.get(x, y) && mask.get(x, y));
		return inverted;
	}
}
