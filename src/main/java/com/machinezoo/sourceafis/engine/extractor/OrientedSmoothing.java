// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.extractor;

import java.util.*;
import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class OrientedSmoothing {
	private static IntPoint[][] lines(int resolution, int radius, double step) {
		IntPoint[][] result = new IntPoint[resolution][];
		for (int orientationIndex = 0; orientationIndex < resolution; ++orientationIndex) {
			List<IntPoint> line = new ArrayList<>();
			line.add(IntPoint.ZERO);
			DoublePoint direction = DoubleAngle.toVector(DoubleAngle.fromOrientation(DoubleAngle.bucketCenter(orientationIndex, resolution)));
			for (double r = radius; r >= 0.5; r /= step) {
				IntPoint sample = direction.multiply(r).round();
				if (!line.contains(sample)) {
					line.add(sample);
					line.add(sample.negate());
				}
			}
			result[orientationIndex] = line.toArray(new IntPoint[line.size()]);
		}
		return result;
	}
	private static DoubleMatrix smoothRidges(DoubleMatrix input, DoubleMatrix orientation, BooleanMatrix mask, BlockMap blocks, double angle, IntPoint[][] lines) {
		DoubleMatrix output = new DoubleMatrix(input.size());
		for (IntPoint block : blocks.primary.blocks) {
			if (mask.get(block)) {
				IntPoint[] line = lines[DoubleAngle.quantize(DoubleAngle.add(orientation.get(block), angle), lines.length)];
				for (IntPoint linePoint : line) {
					IntRect target = blocks.primary.block(block);
					IntRect source = target.move(linePoint).intersect(new IntRect(blocks.pixels));
					target = source.move(linePoint.negate());
					for (int y = target.top(); y < target.bottom(); ++y)
						for (int x = target.left(); x < target.right(); ++x)
							output.add(x, y, input.get(x + linePoint.x, y + linePoint.y));
				}
				IntRect blockArea = blocks.primary.block(block);
				for (int y = blockArea.top(); y < blockArea.bottom(); ++y)
					for (int x = blockArea.left(); x < blockArea.right(); ++x)
						output.multiply(x, y, 1.0 / line.length);
			}
		}
		return output;
	}
	public static DoubleMatrix parallel(DoubleMatrix input, DoubleMatrix orientation, BooleanMatrix mask, BlockMap blocks) {
		var lines = lines(Parameters.PARALLEL_SMOOTHING_RESOLUTION, Parameters.PARALLEL_SMOOTHING_RADIUS, Parameters.PARALLEL_SMOOTHING_STEP);
		var smoothed = smoothRidges(input, orientation, mask, blocks, 0, lines);
		// https://sourceafis.machinezoo.com/transparency/parallel-smoothing
		TransparencySink.current().log("parallel-smoothing", smoothed);
		return smoothed;
	}
	public static DoubleMatrix orthogonal(DoubleMatrix input, DoubleMatrix orientation, BooleanMatrix mask, BlockMap blocks) {
		var lines = lines(Parameters.ORTHOGONAL_SMOOTHING_RESOLUTION, Parameters.ORTHOGONAL_SMOOTHING_RADIUS, Parameters.ORTHOGONAL_SMOOTHING_STEP);
		var smoothed = smoothRidges(input, orientation, mask, blocks, Math.PI, lines);
		// https://sourceafis.machinezoo.com/transparency/orthogonal-smoothing
		TransparencySink.current().log("orthogonal-smoothing", smoothed);
		return smoothed;
	}
}
