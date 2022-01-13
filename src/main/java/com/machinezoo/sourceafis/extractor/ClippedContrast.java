// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor;

import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.primitives.*;
import com.machinezoo.sourceafis.transparency.*;

public class ClippedContrast {
	public static DoubleMatrix compute(BlockMap blocks, HistogramCube histogram) {
		DoubleMatrix result = new DoubleMatrix(blocks.primary.blocks);
		for (IntPoint block : blocks.primary.blocks) {
			int volume = histogram.sum(block);
			int clipLimit = (int)Math.round(volume * Parameters.CLIPPED_CONTRAST);
			int accumulator = 0;
			int lowerBound = histogram.bins - 1;
			for (int i = 0; i < histogram.bins; ++i) {
				accumulator += histogram.get(block, i);
				if (accumulator > clipLimit) {
					lowerBound = i;
					break;
				}
			}
			accumulator = 0;
			int upperBound = 0;
			for (int i = histogram.bins - 1; i >= 0; --i) {
				accumulator += histogram.get(block, i);
				if (accumulator > clipLimit) {
					upperBound = i;
					break;
				}
			}
			result.set(block, (upperBound - lowerBound) * (1.0 / (histogram.bins - 1)));
		}
		// https://sourceafis.machinezoo.com/transparency/contrast
		TransparencySink.current().log("contrast", result);
		return result;
	}
}
