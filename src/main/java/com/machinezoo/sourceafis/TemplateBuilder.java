// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.util.*;
import java.util.stream.*;
import com.google.gson.*;

class TemplateBuilder {
	FingerprintTransparency transparency = FingerprintTransparency.none;
	IntPoint size;
	ImmutableMinutia[] minutiae;
	NeighborEdge[][] edges;
	void extract(byte[] image, double dpi) {
		DoubleMap raw = ImageDecoder.toDoubleMap(image);
		// https://sourceafis.machinezoo.com/transparency/decoded-image
		transparency.logDecodedImage(raw);
		if (Math.abs(dpi - 500) > Parameters.dpiTolerance)
			raw = scaleImage(raw, dpi);
		// https://sourceafis.machinezoo.com/transparency/scaled-image
		transparency.logScaledImage(raw);
		size = raw.size();
		BlockMap blocks = new BlockMap(raw.width, raw.height, Parameters.blockSize);
		// https://sourceafis.machinezoo.com/transparency/block-map
		transparency.logBlockMap(blocks);
		HistogramMap histogram = histogram(blocks, raw);
		HistogramMap smoothHistogram = smoothHistogram(blocks, histogram);
		BooleanMap mask = mask(blocks, histogram);
		DoubleMap equalized = equalize(blocks, raw, smoothHistogram, mask);
		DoubleMap orientation = orientationMap(equalized, mask, blocks);
		IntPoint[][] smoothedLines = orientedLines(Parameters.parallelSmoothinigResolution, Parameters.parallelSmoothinigRadius, Parameters.parallelSmoothinigStep);
		DoubleMap smoothed = smoothRidges(equalized, orientation, mask, blocks, 0, smoothedLines);
		// https://sourceafis.machinezoo.com/transparency/parallel-smoothing
		transparency.logParallelSmoothing(smoothed);
		IntPoint[][] orthogonalLines = orientedLines(Parameters.orthogonalSmoothinigResolution, Parameters.orthogonalSmoothinigRadius, Parameters.orthogonalSmoothinigStep);
		DoubleMap orthogonal = smoothRidges(smoothed, orientation, mask, blocks, Math.PI, orthogonalLines);
		// https://sourceafis.machinezoo.com/transparency/orthogonal-smoothing
		transparency.logOrthogonalSmoothing(orthogonal);
		BooleanMap binary = binarize(smoothed, orthogonal, mask, blocks);
		BooleanMap pixelMask = fillBlocks(mask, blocks);
		cleanupBinarized(binary, pixelMask);
		// https://sourceafis.machinezoo.com/transparency/pixel-mask
		transparency.logPixelMask(pixelMask);
		BooleanMap inverted = invert(binary, pixelMask);
		BooleanMap innerMask = innerMask(pixelMask);
		Skeleton ridges = new Skeleton(binary, SkeletonType.RIDGES, transparency);
		Skeleton valleys = new Skeleton(inverted, SkeletonType.VALLEYS, transparency);
		collectMinutiae(ridges, MinutiaType.ENDING);
		collectMinutiae(valleys, MinutiaType.BIFURCATION);
		// https://sourceafis.machinezoo.com/transparency/skeleton-minutiae
		transparency.logSkeletonMinutiae(this);
		maskMinutiae(innerMask);
		removeMinutiaClouds();
		limitTemplateSize();
		shuffleMinutiae();
		buildEdgeTable();
	}
	void deserialize(String json) {
		JsonTemplate data = new Gson().fromJson(json, JsonTemplate.class);
		data.validate();
		size = data.size();
		minutiae = data.minutiae();
		// https://sourceafis.machinezoo.com/transparency/deserialized-minutiae
		transparency.logDeserializedMinutiae(this);
		buildEdgeTable();
	}
	void convert(ForeignTemplate template, ForeignFingerprint fingerprint) {
		int width = normalizeDpi(fingerprint.dimensions.width, fingerprint.dimensions.dpiX);
		int height = normalizeDpi(fingerprint.dimensions.height, fingerprint.dimensions.dpiY);
		size = new IntPoint(width, height);
		List<ImmutableMinutia> list = new ArrayList<>();
		for (ForeignMinutia minutia : fingerprint.minutiae) {
			int x = normalizeDpi(minutia.x, fingerprint.dimensions.dpiX);
			int y = normalizeDpi(minutia.y, fingerprint.dimensions.dpiY);
			list.add(new ImmutableMinutia(new IntPoint(x, y), minutia.angle, minutia.type.convert()));
		}
		minutiae = list.stream().toArray(ImmutableMinutia[]::new);
		shuffleMinutiae();
		buildEdgeTable();
	}
	private static int normalizeDpi(int value, double dpi) {
		if (Math.abs(dpi - 500) > Parameters.dpiTolerance)
			return (int)Math.round(value / dpi * 500);
		else
			return value;
	}
	static DoubleMap scaleImage(DoubleMap input, double dpi) {
		return scaleImage(input, (int)Math.round(500.0 / dpi * input.width), (int)Math.round(500.0 / dpi * input.height));
	}
	static DoubleMap scaleImage(DoubleMap input, int newWidth, int newHeight) {
		DoubleMap output = new DoubleMap(newWidth, newHeight);
		double scaleX = newWidth / (double)input.width;
		double scaleY = newHeight / (double)input.height;
		double descaleX = 1 / scaleX;
		double descaleY = 1 / scaleY;
		for (int y = 0; y < newHeight; ++y) {
			double y1 = y * descaleY;
			double y2 = y1 + descaleY;
			int y1i = (int)y1;
			int y2i = Math.min((int)Math.ceil(y2), input.height);
			for (int x = 0; x < newWidth; ++x) {
				double x1 = x * descaleX;
				double x2 = x1 + descaleX;
				int x1i = (int)x1;
				int x2i = Math.min((int)Math.ceil(x2), input.width);
				double sum = 0;
				for (int oy = y1i; oy < y2i; ++oy) {
					double ry = Math.min(oy + 1, y2) - Math.max(oy, y1);
					for (int ox = x1i; ox < x2i; ++ox) {
						double rx = Math.min(ox + 1, x2) - Math.max(ox, x1);
						sum += rx * ry * input.get(ox, oy);
					}
				}
				output.set(x, y, sum * (scaleX * scaleY));
			}
		}
		return output;
	}
	private HistogramMap histogram(BlockMap blocks, DoubleMap image) {
		HistogramMap histogram = new HistogramMap(blocks.primary.blocks, Parameters.histogramDepth);
		for (IntPoint block : blocks.primary.blocks) {
			IntRect area = blocks.primary.block(block);
			for (int y = area.top(); y < area.bottom(); ++y)
				for (int x = area.left(); x < area.right(); ++x) {
					int depth = (int)(image.get(x, y) * histogram.depth);
					histogram.increment(block, histogram.constrain(depth));
				}
		}
		// https://sourceafis.machinezoo.com/transparency/histogram
		transparency.logHistogram(histogram);
		return histogram;
	}
	private HistogramMap smoothHistogram(BlockMap blocks, HistogramMap input) {
		IntPoint[] blocksAround = new IntPoint[] { new IntPoint(0, 0), new IntPoint(-1, 0), new IntPoint(0, -1), new IntPoint(-1, -1) };
		HistogramMap output = new HistogramMap(blocks.secondary.blocks, input.depth);
		for (IntPoint corner : blocks.secondary.blocks) {
			for (IntPoint relative : blocksAround) {
				IntPoint block = corner.plus(relative);
				if (blocks.primary.blocks.contains(block)) {
					for (int i = 0; i < input.depth; ++i)
						output.add(corner, i, input.get(block, i));
				}
			}
		}
		// https://sourceafis.machinezoo.com/transparency/smoothed-histogram
		transparency.logSmoothedHistogram(output);
		return output;
	}
	private BooleanMap mask(BlockMap blocks, HistogramMap histogram) {
		DoubleMap contrast = clipContrast(blocks, histogram);
		BooleanMap mask = filterAbsoluteContrast(contrast);
		mask.merge(filterRelativeContrast(contrast, blocks));
		// https://sourceafis.machinezoo.com/transparency/combined-mask
		transparency.logCombinedMask(mask);
		mask.merge(vote(mask, null, Parameters.contrastVoteRadius, Parameters.contrastVoteMajority, Parameters.contrastVoteBorderDistance));
		mask.merge(filterBlockErrors(mask));
		mask.invert();
		mask.merge(filterBlockErrors(mask));
		mask.merge(filterBlockErrors(mask));
		mask.merge(vote(mask, null, Parameters.maskVoteRadius, Parameters.maskVoteMajority, Parameters.maskVoteBorderDistance));
		// https://sourceafis.machinezoo.com/transparency/filtered-mask
		transparency.logFilteredMask(mask);
		return mask;
	}
	private DoubleMap clipContrast(BlockMap blocks, HistogramMap histogram) {
		DoubleMap result = new DoubleMap(blocks.primary.blocks);
		for (IntPoint block : blocks.primary.blocks) {
			int volume = histogram.sum(block);
			int clipLimit = (int)Math.round(volume * Parameters.clippedContrast);
			int accumulator = 0;
			int lowerBound = histogram.depth - 1;
			for (int i = 0; i < histogram.depth; ++i) {
				accumulator += histogram.get(block, i);
				if (accumulator > clipLimit) {
					lowerBound = i;
					break;
				}
			}
			accumulator = 0;
			int upperBound = 0;
			for (int i = histogram.depth - 1; i >= 0; --i) {
				accumulator += histogram.get(block, i);
				if (accumulator > clipLimit) {
					upperBound = i;
					break;
				}
			}
			result.set(block, (upperBound - lowerBound) * (1.0 / (histogram.depth - 1)));
		}
		// https://sourceafis.machinezoo.com/transparency/clipped-contrast
		transparency.logClippedContrast(result);
		return result;
	}
	private BooleanMap filterAbsoluteContrast(DoubleMap contrast) {
		BooleanMap result = new BooleanMap(contrast.size());
		for (IntPoint block : contrast.size())
			if (contrast.get(block) < Parameters.minAbsoluteContrast)
				result.set(block, true);
		// https://sourceafis.machinezoo.com/transparency/absolute-contrast-mask
		transparency.logAbsoluteContrastMask(result);
		return result;
	}
	private BooleanMap filterRelativeContrast(DoubleMap contrast, BlockMap blocks) {
		List<Double> sortedContrast = new ArrayList<>();
		for (IntPoint block : contrast.size())
			sortedContrast.add(contrast.get(block));
		sortedContrast.sort(Comparator.<Double>naturalOrder().reversed());
		int pixelsPerBlock = blocks.pixels.area() / blocks.primary.blocks.area();
		int sampleCount = Math.min(sortedContrast.size(), Parameters.relativeContrastSample / pixelsPerBlock);
		int consideredBlocks = Math.max((int)Math.round(sampleCount * Parameters.relativeContrastPercentile), 1);
		double averageContrast = sortedContrast.stream().mapToDouble(n -> n).limit(consideredBlocks).average().getAsDouble();
		double limit = averageContrast * Parameters.minRelativeContrast;
		BooleanMap result = new BooleanMap(blocks.primary.blocks);
		for (IntPoint block : blocks.primary.blocks)
			if (contrast.get(block) < limit)
				result.set(block, true);
		// https://sourceafis.machinezoo.com/transparency/relative-contrast-mask
		transparency.logRelativeContrastMask(result);
		return result;
	}
	private BooleanMap vote(BooleanMap input, BooleanMap mask, int radius, double majority, int borderDistance) {
		IntPoint size = input.size();
		IntRect rect = new IntRect(borderDistance, borderDistance, size.x - 2 * borderDistance, size.y - 2 * borderDistance);
		int[] thresholds = IntStream.range(0, Integers.sq(2 * radius + 1) + 1).map(i -> (int)Math.ceil(majority * i)).toArray();
		IntMap counts = new IntMap(size);
		BooleanMap output = new BooleanMap(size);
		for (int y = rect.top(); y < rect.bottom(); ++y) {
			int superTop = y - radius - 1;
			int superBottom = y + radius;
			int yMin = Math.max(0, y - radius);
			int yMax = Math.min(size.y - 1, y + radius);
			int yRange = yMax - yMin + 1;
			for (int x = rect.left(); x < rect.right(); ++x)
				if (mask == null || mask.get(x, y)) {
					int left = x > 0 ? counts.get(x - 1, y) : 0;
					int top = y > 0 ? counts.get(x, y - 1) : 0;
					int diagonal = x > 0 && y > 0 ? counts.get(x - 1, y - 1) : 0;
					int xMin = Math.max(0, x - radius);
					int xMax = Math.min(size.x - 1, x + radius);
					int ones;
					if (left > 0 && top > 0 && diagonal > 0) {
						ones = top + left - diagonal - 1;
						int superLeft = x - radius - 1;
						int superRight = x + radius;
						if (superLeft >= 0 && superTop >= 0 && input.get(superLeft, superTop))
							++ones;
						if (superLeft >= 0 && superBottom < size.y && input.get(superLeft, superBottom))
							--ones;
						if (superRight < size.x && superTop >= 0 && input.get(superRight, superTop))
							--ones;
						if (superRight < size.x && superBottom < size.y && input.get(superRight, superBottom))
							++ones;
					} else {
						ones = 0;
						for (int ny = yMin; ny <= yMax; ++ny)
							for (int nx = xMin; nx <= xMax; ++nx)
								if (input.get(nx, ny))
									++ones;
					}
					counts.set(x, y, ones + 1);
					if (ones >= thresholds[yRange * (xMax - xMin + 1)])
						output.set(x, y, true);
				}
		}
		return output;
	}
	private BooleanMap filterBlockErrors(BooleanMap input) {
		return vote(input, null, Parameters.blockErrorsVoteRadius, Parameters.blockErrorsVoteMajority, Parameters.blockErrorsVoteBorderDistance);
	}
	private DoubleMap equalize(BlockMap blocks, DoubleMap image, HistogramMap histogram, BooleanMap blockMask) {
		final double rangeMin = -1;
		final double rangeMax = 1;
		final double rangeSize = rangeMax - rangeMin;
		final double widthMax = rangeSize / 256 * Parameters.maxEqualizationScaling;
		final double widthMin = rangeSize / 256 * Parameters.minEqualizationScaling;
		double[] limitedMin = new double[histogram.depth];
		double[] limitedMax = new double[histogram.depth];
		double[] dequantized = new double[histogram.depth];
		for (int i = 0; i < histogram.depth; ++i) {
			limitedMin[i] = Math.max(i * widthMin + rangeMin, rangeMax - (histogram.depth - 1 - i) * widthMax);
			limitedMax[i] = Math.min(i * widthMax + rangeMin, rangeMax - (histogram.depth - 1 - i) * widthMin);
			dequantized[i] = i / (double)(histogram.depth - 1);
		}
		Map<IntPoint, double[]> mappings = new HashMap<>();
		for (IntPoint corner : blocks.secondary.blocks) {
			double[] mapping = new double[histogram.depth];
			mappings.put(corner, mapping);
			if (blockMask.get(corner, false) || blockMask.get(corner.x - 1, corner.y, false)
				|| blockMask.get(corner.x, corner.y - 1, false) || blockMask.get(corner.x - 1, corner.y - 1, false)) {
				double step = rangeSize / histogram.sum(corner);
				double top = rangeMin;
				for (int i = 0; i < histogram.depth; ++i) {
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
		DoubleMap result = new DoubleMap(blocks.pixels);
		for (IntPoint block : blocks.primary.blocks) {
			IntRect area = blocks.primary.block(block);
			if (blockMask.get(block)) {
				double[] topleft = mappings.get(block);
				double[] topright = mappings.get(new IntPoint(block.x + 1, block.y));
				double[] bottomleft = mappings.get(new IntPoint(block.x, block.y + 1));
				double[] bottomright = mappings.get(new IntPoint(block.x + 1, block.y + 1));
				for (int y = area.top(); y < area.bottom(); ++y)
					for (int x = area.left(); x < area.right(); ++x) {
						int depth = histogram.constrain((int)(image.get(x, y) * histogram.depth));
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
		transparency.logEqualizedImage(result);
		return result;
	}
	private DoubleMap orientationMap(DoubleMap image, BooleanMap mask, BlockMap blocks) {
		DoublePointMap accumulated = pixelwiseOrientation(image, mask, blocks);
		DoublePointMap byBlock = blockOrientations(accumulated, blocks, mask);
		DoublePointMap smooth = smoothOrientation(byBlock, mask);
		return orientationAngles(smooth, mask);
	}
	private static class ConsideredOrientation {
		IntPoint offset;
		DoublePoint orientation;
	}
	private static class OrientationRandom {
		static final int prime = 1610612741;
		static final int bits = 30;
		static final int mask = (1 << bits) - 1;
		static final double scaling = 1.0 / (1 << bits);
		long state = prime * prime * prime;
		double next() {
			state *= prime;
			return ((state & mask) + 0.5) * scaling;
		}
	}
	private ConsideredOrientation[][] planOrientations() {
		OrientationRandom random = new OrientationRandom();
		ConsideredOrientation[][] splits = new ConsideredOrientation[Parameters.orientationSplit][];
		for (int i = 0; i < Parameters.orientationSplit; ++i) {
			ConsideredOrientation[] orientations = splits[i] = new ConsideredOrientation[Parameters.orientationsChecked];
			for (int j = 0; j < Parameters.orientationsChecked; ++j) {
				ConsideredOrientation sample = orientations[j] = new ConsideredOrientation();
				do {
					double angle = random.next() * Math.PI;
					double distance = Doubles.interpolateExponential(Parameters.minOrientationRadius, Parameters.maxOrientationRadius, random.next());
					sample.offset = DoubleAngle.toVector(angle).multiply(distance).round();
				} while (sample.offset.equals(IntPoint.zero) || sample.offset.y < 0 || Arrays.stream(orientations).limit(j).anyMatch(o -> o.offset.equals(sample.offset)));
				sample.orientation = DoubleAngle.toVector(DoubleAngle.add(DoubleAngle.toOrientation(DoubleAngle.atan(sample.offset.toPoint())), Math.PI));
			}
		}
		return splits;
	}
	private DoublePointMap pixelwiseOrientation(DoubleMap input, BooleanMap mask, BlockMap blocks) {
		ConsideredOrientation[][] neighbors = planOrientations();
		DoublePointMap orientation = new DoublePointMap(input.size());
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
		transparency.logPixelwiseOrientation(orientation);
		return orientation;
	}
	private static IntRange maskRange(BooleanMap mask, int y) {
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
			return IntRange.zero;
	}
	private DoublePointMap blockOrientations(DoublePointMap orientation, BlockMap blocks, BooleanMap mask) {
		DoublePointMap sums = new DoublePointMap(blocks.primary.blocks);
		for (IntPoint block : blocks.primary.blocks) {
			if (mask.get(block)) {
				IntRect area = blocks.primary.block(block);
				for (int y = area.top(); y < area.bottom(); ++y)
					for (int x = area.left(); x < area.right(); ++x)
						sums.add(block, orientation.get(x, y));
			}
		}
		// https://sourceafis.machinezoo.com/transparency/block-orientation
		transparency.logBlockOrientation(sums);
		return sums;
	}
	private DoublePointMap smoothOrientation(DoublePointMap orientation, BooleanMap mask) {
		IntPoint size = mask.size();
		DoublePointMap smoothed = new DoublePointMap(size);
		for (IntPoint block : size)
			if (mask.get(block)) {
				IntRect neighbors = IntRect.around(block, Parameters.orientationSmoothingRadius).intersect(new IntRect(size));
				for (int ny = neighbors.top(); ny < neighbors.bottom(); ++ny)
					for (int nx = neighbors.left(); nx < neighbors.right(); ++nx)
						if (mask.get(nx, ny))
							smoothed.add(block, orientation.get(nx, ny));
			}
		// https://sourceafis.machinezoo.com/transparency/smoothed-orientation
		transparency.logSmoothedOrientation(smoothed);
		return smoothed;
	}
	private static DoubleMap orientationAngles(DoublePointMap vectors, BooleanMap mask) {
		IntPoint size = mask.size();
		DoubleMap angles = new DoubleMap(size);
		for (IntPoint block : size)
			if (mask.get(block))
				angles.set(block, DoubleAngle.atan(vectors.get(block)));
		return angles;
	}
	private IntPoint[][] orientedLines(int resolution, int radius, double step) {
		IntPoint[][] result = new IntPoint[resolution][];
		for (int orientationIndex = 0; orientationIndex < resolution; ++orientationIndex) {
			List<IntPoint> line = new ArrayList<>();
			line.add(IntPoint.zero);
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
	private static DoubleMap smoothRidges(DoubleMap input, DoubleMap orientation, BooleanMap mask, BlockMap blocks, double angle, IntPoint[][] lines) {
		DoubleMap output = new DoubleMap(input.size());
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
	private BooleanMap binarize(DoubleMap input, DoubleMap baseline, BooleanMap mask, BlockMap blocks) {
		IntPoint size = input.size();
		BooleanMap binarized = new BooleanMap(size);
		for (IntPoint block : blocks.primary.blocks)
			if (mask.get(block)) {
				IntRect rect = blocks.primary.block(block);
				for (int y = rect.top(); y < rect.bottom(); ++y)
					for (int x = rect.left(); x < rect.right(); ++x)
						if (input.get(x, y) - baseline.get(x, y) > 0)
							binarized.set(x, y, true);
			}
		// https://sourceafis.machinezoo.com/transparency/binarized-image
		transparency.logBinarizedImage(binarized);
		return binarized;
	}
	private void cleanupBinarized(BooleanMap binary, BooleanMap mask) {
		IntPoint size = binary.size();
		BooleanMap inverted = new BooleanMap(binary);
		inverted.invert();
		BooleanMap islands = vote(inverted, mask, Parameters.binarizedVoteRadius, Parameters.binarizedVoteMajority, Parameters.binarizedVoteBorderDistance);
		BooleanMap holes = vote(binary, mask, Parameters.binarizedVoteRadius, Parameters.binarizedVoteMajority, Parameters.binarizedVoteBorderDistance);
		for (int y = 0; y < size.y; ++y)
			for (int x = 0; x < size.x; ++x)
				binary.set(x, y, binary.get(x, y) && !islands.get(x, y) || holes.get(x, y));
		removeCrosses(binary);
		// https://sourceafis.machinezoo.com/transparency/filtered-binary-image
		transparency.logFilteredBinarydImage(binary);
	}
	private static void removeCrosses(BooleanMap input) {
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
	private static BooleanMap fillBlocks(BooleanMap mask, BlockMap blocks) {
		BooleanMap pixelized = new BooleanMap(blocks.pixels);
		for (IntPoint block : blocks.primary.blocks)
			if (mask.get(block))
				for (IntPoint pixel : blocks.primary.block(block))
					pixelized.set(pixel, true);
		return pixelized;
	}
	private static BooleanMap invert(BooleanMap binary, BooleanMap mask) {
		IntPoint size = binary.size();
		BooleanMap inverted = new BooleanMap(size);
		for (int y = 0; y < size.y; ++y)
			for (int x = 0; x < size.x; ++x)
				inverted.set(x, y, !binary.get(x, y) && mask.get(x, y));
		return inverted;
	}
	private BooleanMap innerMask(BooleanMap outer) {
		IntPoint size = outer.size();
		BooleanMap inner = new BooleanMap(size);
		for (int y = 1; y < size.y - 1; ++y)
			for (int x = 1; x < size.x - 1; ++x)
				inner.set(x, y, outer.get(x, y));
		if (Parameters.innerMaskBorderDistance >= 1)
			inner = shrinkMask(inner, 1);
		int total = 1;
		for (int step = 1; total + step <= Parameters.innerMaskBorderDistance; step *= 2) {
			inner = shrinkMask(inner, step);
			total += step;
		}
		if (total < Parameters.innerMaskBorderDistance)
			inner = shrinkMask(inner, Parameters.innerMaskBorderDistance - total);
		// https://sourceafis.machinezoo.com/transparency/inner-mask
		transparency.logInnerMask(inner);
		return inner;
	}
	private static BooleanMap shrinkMask(BooleanMap mask, int amount) {
		IntPoint size = mask.size();
		BooleanMap shrunk = new BooleanMap(size);
		for (int y = amount; y < size.y - amount; ++y)
			for (int x = amount; x < size.x - amount; ++x)
				shrunk.set(x, y, mask.get(x, y - amount) && mask.get(x, y + amount) && mask.get(x - amount, y) && mask.get(x + amount, y));
		return shrunk;
	}
	private void collectMinutiae(Skeleton skeleton, MinutiaType type) {
		minutiae = Stream.concat(
			Arrays.stream(Optional.ofNullable(minutiae).orElse(new ImmutableMinutia[0])),
			skeleton.minutiae.stream()
				.filter(m -> m.ridges.size() == 1)
				.map(m -> new ImmutableMinutia(m.position, m.ridges.get(0).direction(), type)))
			.toArray(ImmutableMinutia[]::new);
	}
	private void maskMinutiae(BooleanMap mask) {
		minutiae = Arrays.stream(minutiae)
			.filter(minutia -> {
				IntPoint arrow = DoubleAngle.toVector(minutia.direction).multiply(-Parameters.maskDisplacement).round();
				return mask.get(minutia.position.plus(arrow), false);
			})
			.toArray(ImmutableMinutia[]::new);
		// https://sourceafis.machinezoo.com/transparency/inner-minutiae
		transparency.logInnerMinutiae(this);
	}
	private void removeMinutiaClouds() {
		int radiusSq = Integers.sq(Parameters.minutiaCloudRadius);
		Set<ImmutableMinutia> removed = Arrays.stream(minutiae)
			.filter(minutia -> Parameters.maxCloudSize < Arrays.stream(minutiae)
				.filter(neighbor -> neighbor.position.minus(minutia.position).lengthSq() <= radiusSq)
				.count() - 1)
			.collect(toSet());
		minutiae = Arrays.stream(minutiae)
			.filter(minutia -> !removed.contains(minutia))
			.toArray(ImmutableMinutia[]::new);
		// https://sourceafis.machinezoo.com/transparency/removed-minutia-clouds
		transparency.logRemovedMinutiaClouds(this);
	}
	private void limitTemplateSize() {
		if (minutiae.length > Parameters.maxMinutiae) {
			minutiae = Arrays.stream(minutiae)
				.sorted(Comparator.<ImmutableMinutia>comparingInt(
					minutia -> Arrays.stream(minutiae)
						.mapToInt(neighbor -> minutia.position.minus(neighbor.position).lengthSq())
						.sorted()
						.skip(Parameters.sortByNeighbor)
						.findFirst().orElse(Integer.MAX_VALUE))
					.reversed())
				.limit(Parameters.maxMinutiae)
				.toArray(ImmutableMinutia[]::new);
		}
		// https://sourceafis.machinezoo.com/transparency/top-minutiae
		transparency.logTopMinutiae(this);
	}
	private void shuffleMinutiae() {
		int prime = 1610612741;
		Arrays.sort(minutiae, Comparator
			.comparingInt((ImmutableMinutia m) -> ((m.position.x * prime) + m.position.y) * prime)
			.thenComparing(m -> m.position.x)
			.thenComparing(m -> m.position.y)
			.thenComparing(m -> m.direction)
			.thenComparing(m -> m.type));
		// https://sourceafis.machinezoo.com/transparency/shuffled-minutiae
		transparency.logShuffledMinutiae(this);
	}
	private void buildEdgeTable() {
		edges = new NeighborEdge[minutiae.length][];
		List<NeighborEdge> star = new ArrayList<>();
		int[] allSqDistances = new int[minutiae.length];
		for (int reference = 0; reference < edges.length; ++reference) {
			IntPoint referencePosition = minutiae[reference].position;
			int sqMaxDistance = Integers.sq(Parameters.edgeTableRange);
			if (minutiae.length - 1 > Parameters.edgeTableNeighbors) {
				for (int neighbor = 0; neighbor < minutiae.length; ++neighbor)
					allSqDistances[neighbor] = referencePosition.minus(minutiae[neighbor].position).lengthSq();
				Arrays.sort(allSqDistances);
				sqMaxDistance = allSqDistances[Parameters.edgeTableNeighbors];
			}
			for (int neighbor = 0; neighbor < minutiae.length; ++neighbor) {
				if (neighbor != reference && referencePosition.minus(minutiae[neighbor].position).lengthSq() <= sqMaxDistance)
					star.add(new NeighborEdge(minutiae, reference, neighbor));
			}
			star.sort(Comparator.<NeighborEdge>comparingInt(e -> e.length).thenComparingInt(e -> e.neighbor));
			while (star.size() > Parameters.edgeTableNeighbors)
				star.remove(star.size() - 1);
			edges[reference] = star.toArray(new NeighborEdge[star.size()]);
			star.clear();
		}
		// https://sourceafis.machinezoo.com/transparency/edge-table
		transparency.logEdgeTable(edges);
	}
}
