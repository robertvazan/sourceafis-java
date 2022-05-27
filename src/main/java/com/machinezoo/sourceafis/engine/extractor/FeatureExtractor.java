// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.extractor;

import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.extractor.minutiae.*;
import com.machinezoo.sourceafis.engine.extractor.skeletons.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.templates.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class FeatureExtractor {
	public static MutableTemplate extract(DoubleMatrix raw, double dpi) {
		MutableTemplate template = new MutableTemplate();
		// https://sourceafis.machinezoo.com/transparency/decoded-image
		TransparencySink.current().log("decoded-image", raw);
		raw = ImageResizer.resize(raw, dpi);
		// https://sourceafis.machinezoo.com/transparency/scaled-image
		TransparencySink.current().log("scaled-image", raw);
		template.size = raw.size();
		BlockMap blocks = new BlockMap(raw.width, raw.height, Parameters.BLOCK_SIZE);
		// https://sourceafis.machinezoo.com/transparency/blocks
		TransparencySink.current().log("blocks", blocks);
		HistogramCube histogram = LocalHistograms.create(blocks, raw);
		HistogramCube smoothHistogram = LocalHistograms.smooth(blocks, histogram);
		BooleanMatrix mask = SegmentationMask.compute(blocks, histogram);
		DoubleMatrix equalized = ImageEqualization.equalize(blocks, raw, smoothHistogram, mask);
		DoubleMatrix orientation = BlockOrientations.compute(equalized, mask, blocks);
		DoubleMatrix smoothed = OrientedSmoothing.parallel(equalized, orientation, mask, blocks);
		DoubleMatrix orthogonal = OrientedSmoothing.orthogonal(smoothed, orientation, mask, blocks);
		BooleanMatrix binary = BinarizedImage.binarize(smoothed, orthogonal, mask, blocks);
		BooleanMatrix pixelMask = SegmentationMask.pixelwise(mask, blocks);
		BinarizedImage.cleanup(binary, pixelMask);
		BooleanMatrix inverted = BinarizedImage.invert(binary, pixelMask);
		BooleanMatrix innerMask = SegmentationMask.inner(pixelMask);
		Skeleton ridges = Skeletons.create(binary, SkeletonType.RIDGES);
		Skeleton valleys = Skeletons.create(inverted, SkeletonType.VALLEYS);
		template.minutiae = MinutiaCollector.collect(ridges, valleys);
		// https://sourceafis.machinezoo.com/transparency/skeleton-minutiae
		TransparencySink.current().log("skeleton-minutiae", template);
		InnerMinutiaeFilter.apply(template.minutiae, innerMask);
		// https://sourceafis.machinezoo.com/transparency/inner-minutiae
		TransparencySink.current().log("inner-minutiae", template);
		MinutiaCloudFilter.apply(template.minutiae);
		// https://sourceafis.machinezoo.com/transparency/removed-minutia-clouds
		TransparencySink.current().log("removed-minutia-clouds", template);
		template.minutiae = TopMinutiaeFilter.apply(template.minutiae);
		// https://sourceafis.machinezoo.com/transparency/top-minutiae
		TransparencySink.current().log("top-minutiae", template);
		return template;
	}
}
