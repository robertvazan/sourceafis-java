// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor;

import java.util.*;
import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.primitives.*;
import com.machinezoo.sourceafis.transparency.*;

public class RelativeContrastMask {
	public static BooleanMatrix compute(DoubleMatrix contrast, BlockMap blocks) {
		List<Double> sortedContrast = new ArrayList<>();
		for (IntPoint block : contrast.size())
			sortedContrast.add(contrast.get(block));
		sortedContrast.sort(Comparator.<Double>naturalOrder().reversed());
		int pixelsPerBlock = blocks.pixels.area() / blocks.primary.blocks.area();
		int sampleCount = Math.min(sortedContrast.size(), Parameters.RELATIVE_CONTRAST_SAMPLE / pixelsPerBlock);
		int consideredBlocks = Math.max((int)Math.round(sampleCount * Parameters.RELATIVE_CONTRAST_PERCENTILE), 1);
		double averageContrast = sortedContrast.stream().mapToDouble(n -> n).limit(consideredBlocks).average().getAsDouble();
		double limit = averageContrast * Parameters.MIN_RELATIVE_CONTRAST;
		BooleanMatrix result = new BooleanMatrix(blocks.primary.blocks);
		for (IntPoint block : blocks.primary.blocks)
			if (contrast.get(block) < limit)
				result.set(block, true);
		// https://sourceafis.machinezoo.com/transparency/relative-contrast-mask
		TransparencySink.current().log("relative-contrast-mask", result);
		return result;
	}
}
