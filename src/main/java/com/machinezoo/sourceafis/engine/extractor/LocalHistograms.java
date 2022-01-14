// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.extractor;

import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class LocalHistograms {
	public static HistogramCube create(BlockMap blocks, DoubleMatrix image) {
		HistogramCube histogram = new HistogramCube(blocks.primary.blocks, Parameters.HISTOGRAM_DEPTH);
		for (IntPoint block : blocks.primary.blocks) {
			IntRect area = blocks.primary.block(block);
			for (int y = area.top(); y < area.bottom(); ++y)
				for (int x = area.left(); x < area.right(); ++x) {
					int depth = (int)(image.get(x, y) * histogram.bins);
					histogram.increment(block, histogram.constrain(depth));
				}
		}
		// https://sourceafis.machinezoo.com/transparency/histogram
		TransparencySink.current().log("histogram", histogram);
		return histogram;
	}
	public static HistogramCube smooth(BlockMap blocks, HistogramCube input) {
		IntPoint[] blocksAround = new IntPoint[] { new IntPoint(0, 0), new IntPoint(-1, 0), new IntPoint(0, -1), new IntPoint(-1, -1) };
		HistogramCube output = new HistogramCube(blocks.secondary.blocks, input.bins);
		for (IntPoint corner : blocks.secondary.blocks) {
			for (IntPoint relative : blocksAround) {
				IntPoint block = corner.plus(relative);
				if (blocks.primary.blocks.contains(block)) {
					for (int i = 0; i < input.bins; ++i)
						output.add(corner, i, input.get(block, i));
				}
			}
		}
		// https://sourceafis.machinezoo.com/transparency/smoothed-histogram
		TransparencySink.current().log("smoothed-histogram", output);
		return output;
	}
}
