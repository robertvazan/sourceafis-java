// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor;

import java.util.*;
import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.primitives.*;
import com.machinezoo.sourceafis.transparency.*;

public class ImageEqualization {
	public static DoubleMatrix equalize(BlockMap blocks, DoubleMatrix image, HistogramCube histogram, BooleanMatrix blockMask) {
		final double rangeMin = -1;
		final double rangeMax = 1;
		final double rangeSize = rangeMax - rangeMin;
		final double widthMax = rangeSize / 256 * Parameters.MAX_EQUALIZATION_SCALING;
		final double widthMin = rangeSize / 256 * Parameters.MIN_EQUALIZATION_SCALING;
		double[] limitedMin = new double[histogram.bins];
		double[] limitedMax = new double[histogram.bins];
		double[] dequantized = new double[histogram.bins];
		for (int i = 0; i < histogram.bins; ++i) {
			limitedMin[i] = Math.max(i * widthMin + rangeMin, rangeMax - (histogram.bins - 1 - i) * widthMax);
			limitedMax[i] = Math.min(i * widthMax + rangeMin, rangeMax - (histogram.bins - 1 - i) * widthMin);
			dequantized[i] = i / (double)(histogram.bins - 1);
		}
		Map<IntPoint, double[]> mappings = new HashMap<>();
		for (IntPoint corner : blocks.secondary.blocks) {
			double[] mapping = new double[histogram.bins];
			mappings.put(corner, mapping);
			if (blockMask.get(corner, false) || blockMask.get(corner.x - 1, corner.y, false)
				|| blockMask.get(corner.x, corner.y - 1, false) || blockMask.get(corner.x - 1, corner.y - 1, false)) {
				double step = rangeSize / histogram.sum(corner);
				double top = rangeMin;
				for (int i = 0; i < histogram.bins; ++i) {
					double band = histogram.get(corner, i) * step;
					double equalized = top + dequantized[i] * band;
					top += band;
					if (equalized < limitedMin[i])
						equalized = limitedMin[i];
					if (equalized > limitedMax[i])
						equalized = limitedMax[i];
					mapping[i] = equalized;
				}
			}
		}
		DoubleMatrix result = new DoubleMatrix(blocks.pixels);
		for (IntPoint block : blocks.primary.blocks) {
			IntRect area = blocks.primary.block(block);
			if (blockMask.get(block)) {
				double[] topleft = mappings.get(block);
				double[] topright = mappings.get(new IntPoint(block.x + 1, block.y));
				double[] bottomleft = mappings.get(new IntPoint(block.x, block.y + 1));
				double[] bottomright = mappings.get(new IntPoint(block.x + 1, block.y + 1));
				for (int y = area.top(); y < area.bottom(); ++y)
					for (int x = area.left(); x < area.right(); ++x) {
						int depth = histogram.constrain((int)(image.get(x, y) * histogram.bins));
						double rx = (x - area.x + 0.5) / area.width;
						double ry = (y - area.y + 0.5) / area.height;
						result.set(x, y, Doubles.interpolate(bottomleft[depth], bottomright[depth], topleft[depth], topright[depth], rx, ry));
					}
			} else {
				for (int y = area.top(); y < area.bottom(); ++y)
					for (int x = area.left(); x < area.right(); ++x)
						result.set(x, y, -1);
			}
		}
		// https://sourceafis.machinezoo.com/transparency/equalized-image
		TransparencySink.current().log("equalized-image", result);
		return result;
	}
}
