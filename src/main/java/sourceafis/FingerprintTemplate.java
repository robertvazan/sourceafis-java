package sourceafis;

import java.util.*;
import sourceafis.collections.*;
import sourceafis.scalars.*;

public class FingerprintTemplate {
	public FingerprintTemplate(DoubleMap image) {
		this(image, 500);
	}
	public FingerprintTemplate(DoubleMap image, double dpi) {
		final int blockSize = 15;
		final double dpiTolerance = 10;
		if (Math.abs(dpi - 500) > dpiTolerance)
			image = scaleImage(image, dpi);
		BlockMap blocks = new BlockMap(image.width, image.height, blockSize);
		Histogram histogram = histogram(blocks, image);
		Histogram smoothHistogram = smoothHistogram(blocks, histogram);
		BooleanMap mask = mask(blocks, histogram);
		DoubleMap equalized = equalize(blocks, image, smoothHistogram, mask);
		DoubleMap orientation = orientationMap(equalized, mask, blocks);
		DoubleMap smoothed = smoothRidges(equalized, orientation, mask, blocks, 0, orientedLines(new OrientedLineParams().step(1.59)));
		DoubleMap orthogonal = smoothRidges(smoothed, orientation, mask, blocks, Math.PI, orientedLines(new OrientedLineParams().resolution(11).radius(4).step(1.11)));
		BooleanMap binary = binarize(smoothed, orthogonal, mask, blocks);
		cleanupBinarized(binary);
		BooleanMap pixelMask = fillBlocks(mask, blocks);
		BooleanMap inverted = invert(binary, pixelMask);
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
			int y2i = (int)Math.ceil(y2);
			for (int x = 0; x < newWidth; ++x) {
				double x1 = x * descaleX;
				double x2 = x1 + descaleX;
				int x1i = (int)x1;
				int x2i = (int)Math.ceil(x2);
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
	static Histogram histogram(BlockMap blocks, DoubleMap image) {
		final int histogramDepth = 256;
		Histogram histogram = new Histogram(blocks.blockCount.y, blocks.blockCount.x, histogramDepth);
		for (Cell block : blocks.blockCount) {
			Block area = blocks.blockAreas.get(block);
			for (int y = area.bottom(); y < area.top(); ++y)
				for (int x = area.left(); x < area.right(); ++x) {
					int depth = (int)(image.get(x, y) * histogram.depth);
					histogram.increment(block, histogram.constrain(depth));
				}
		}
		return histogram;
	}
	static Histogram smoothHistogram(BlockMap blocks, Histogram input) {
		Cell[] blocksAround = new Cell[] { new Cell(0, 0), new Cell(-1, 0), new Cell(0, -1), new Cell(-1, -1) };
		Histogram output = new Histogram(blocks.cornerCount.y, blocks.cornerCount.x, input.depth);
		for (Cell corner : blocks.cornerCount) {
			for (Cell relative : blocksAround) {
				Cell block = corner.plus(relative);
				if (blocks.blockCount.contains(block)) {
					for (int i = 0; i < input.depth; ++i)
						output.add(corner, i, input.get(block, i));
				}
			}
		}
		return output;
	}
	static BooleanMap mask(BlockMap blocks, Histogram histogram) {
		DoubleMap contrast = clipContrast(blocks, histogram);
		BooleanMap mask = filterAbsoluteContrast(contrast);
		mask.merge(filterRelativeContrast(contrast, blocks));
		mask.merge(vote(mask, new VotingParameters().radius(9).majority(0.86).borderDist(7)));
		mask.merge(filterBlockErrors(mask));
		mask.invert();
		mask.merge(filterBlockErrors(mask));
		mask.merge(filterBlockErrors(mask));
		mask.merge(vote(mask, new VotingParameters().radius(7).borderDist(4)));
		return mask;
	}
	static DoubleMap clipContrast(BlockMap blocks, Histogram histogram) {
		final double clipFraction = 0.08;
		DoubleMap result = new DoubleMap(blocks.blockCount);
		for (Cell block : blocks.blockCount) {
			int volume = histogram.sum(block);
			int clipLimit = (int)Math.round(volume * clipFraction);
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
		return result;
	}
	static BooleanMap filterAbsoluteContrast(DoubleMap contrast) {
		final int limit = 17;
		BooleanMap result = new BooleanMap(contrast.size());
		for (Cell block : contrast.size())
			if (contrast.get(block) < limit)
				result.set(block, true);
		return result;
	}
	static BooleanMap filterRelativeContrast(DoubleMap contrast, BlockMap blocks) {
		final int sampleSize = 168568;
		final double sampleFraction = 0.49;
		final double relativeLimit = 0.34;
		List<Double> sortedContrast = new ArrayList<>();
		for (Cell block : contrast.size())
			sortedContrast.add(contrast.get(block));
		sortedContrast.sort(Comparator.<Double> naturalOrder().reversed());
		int pixelsPerBlock = blocks.pixelCount.area() / blocks.blockCount.area();
		int sampleCount = Math.min(sortedContrast.size(), sampleSize / pixelsPerBlock);
		int consideredBlocks = Math.max((int)Math.round(sampleCount * sampleFraction), 1);
		double averageContrast = sortedContrast.stream().mapToDouble(n -> n).limit(consideredBlocks).average().getAsDouble();
		double limit = averageContrast * relativeLimit;
		BooleanMap result = new BooleanMap(blocks.blockCount);
		for (Cell block : blocks.blockCount)
			if (contrast.get(block) < limit)
				result.set(block, true);
		return result;
	}
	static class VotingParameters {
		int radius = 1;
		double majority = 0.51;
		int borderDist = 0;
		public VotingParameters radius(int radius) {
			this.radius = radius;
			return this;
		}
		public VotingParameters majority(double majority) {
			this.majority = majority;
			return this;
		}
		public VotingParameters borderDist(int borderDist) {
			this.borderDist = borderDist;
			return this;
		}
	}
	static BooleanMap vote(BooleanMap input, VotingParameters args) {
		Cell size = input.size();
		Block rect = new Block(args.borderDist, args.borderDist, size.x - 2 * args.borderDist, size.y - 2 * args.borderDist);
		BooleanMap output = new BooleanMap(size);
		for (Cell center : rect) {
			Block neighborhood = Block.around(center, args.radius).intersect(new Block(size));
			int ones = 0;
			for (int ny = neighborhood.bottom(); ny < neighborhood.top(); ++ny)
				for (int nx = neighborhood.left(); nx < neighborhood.right(); ++nx)
					if (input.get(nx, ny))
						++ones;
			double voteWeight = 1.0 / neighborhood.area();
			if (ones * voteWeight >= args.majority)
				output.set(center, true);
		}
		return output;
	}
	static BooleanMap filterBlockErrors(BooleanMap input) {
		return vote(input, new VotingParameters().majority(0.7).borderDist(4));
	}
	static DoubleMap equalize(BlockMap blocks, DoubleMap image, Histogram histogram, BooleanMap blockMask) {
		final double maxScaling = 3.99;
		final double minScaling = 0.25;
		final double rangeMin = -1;
		final double rangeMax = 1;
		final double rangeSize = rangeMax - rangeMin;
		final double widthMax = rangeSize / 256 * maxScaling;
		final double widthMin = rangeSize / 256 * minScaling;
		double[] limitedMin = new double[histogram.depth];
		double[] limitedMax = new double[histogram.depth];
		double[] dequantized = new double[histogram.depth];
		for (int i = 0; i < histogram.depth; ++i) {
			limitedMin[i] = Math.max(i * widthMin + rangeMin, rangeMax - (histogram.depth - 1 - i) * widthMax);
			limitedMax[i] = Math.min(i * widthMax + rangeMin, rangeMax - (histogram.depth - 1 - i) * widthMin);
			dequantized[i] = i / (double)(histogram.depth - 1);
		}
		Map<Cell, double[]> mappings = new HashMap<>();
		for (Cell corner : blocks.cornerCount) {
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
		DoubleMap result = new DoubleMap(blocks.pixelCount);
		for (Cell block : blocks.blockCount) {
			if (blockMask.get(block)) {
				Block area = blocks.blockAreas.get(block);
				double[] bottomleft = mappings.get(block);
				double[] bottomright = mappings.get(new Cell(block.x + 1, block.y));
				double[] topleft = mappings.get(new Cell(block.x, block.y + 1));
				double[] topright = mappings.get(new Cell(block.x + 1, block.y + 1));
				for (int y = area.bottom(); y < area.top(); ++y)
					for (int x = area.left(); x < area.right(); ++x) {
						int depth = histogram.constrain((int)(image.get(x, y) * histogram.depth));
						double rx = (x - area.x + 0.5) / area.width;
						double ry = (y - area.y + 0.5) / area.height;
						result.set(x, y, Doubles.interpolate(topleft[depth], topright[depth], bottomleft[depth], bottomright[depth], rx, ry));
					}
			}
		}
		return result;
	}
	static DoubleMap orientationMap(DoubleMap image, BooleanMap mask, BlockMap blocks) {
		PointMap accumulated = pixelwiseOrientation(image, mask, blocks);
		PointMap byBlock = blockOrientations(accumulated, blocks, mask);
		PointMap smooth = smoothOrientation(byBlock, mask);
		return orientationAngles(smooth, mask);
	}
	static class ConsideredOrientation {
		Cell offset;
		Point orientation;
	}
	static ConsideredOrientation[][] planOrientations() {
		final double minHalfDistance = 2;
		final double maxHalfDistance = 6;
		final int orientationListSplit = 50;
		final int orientationsChecked = 20;
		Random random = new Random(0);
		ConsideredOrientation[][] splits = new ConsideredOrientation[orientationListSplit][];
		for (int i = 0; i < orientationListSplit; ++i) {
			ConsideredOrientation[] orientations = splits[i] = new ConsideredOrientation[orientationsChecked];
			for (int j = 0; j < orientationsChecked; ++j) {
				ConsideredOrientation sample = orientations[j] = new ConsideredOrientation();
				do {
					double angle = random.nextDouble() * Math.PI;
					double distance = Doubles.interpolateExponential(minHalfDistance, maxHalfDistance, random.nextDouble());
					sample.offset = Angle.toVector(angle).multiply(distance).round();
				} while (sample.offset.equals(Cell.zero) || sample.offset.y < 0 || Arrays.stream(orientations).limit(j).anyMatch(o -> o.offset.equals(sample.offset)));
				sample.orientation = Angle.toVector(Angle.add(Angle.toOrientation(Angle.atan(sample.offset.toPoint())), Math.PI));
			}
		}
		return splits;
	}
	static PointMap pixelwiseOrientation(DoubleMap input, BooleanMap mask, BlockMap blocks) {
		ConsideredOrientation[][] neighbors = planOrientations();
		PointMap orientation = new PointMap(input.size());
		for (int blockY = 0; blockY < blocks.blockCount.y; ++blockY) {
			Range maskRange = maskRange(mask, blockY);
			if (maskRange.length() > 0) {
				Range validXRange = new Range(
					blocks.blockAreas.get(maskRange.start, blockY).left(),
					blocks.blockAreas.get(maskRange.end - 1, blockY).right());
				for (int y = blocks.blockAreas.get(0, blockY).bottom(); y < blocks.blockAreas.get(0, blockY).top(); ++y) {
					for (ConsideredOrientation neighbor : neighbors[y % neighbors.length]) {
						int radius = Math.max(Math.abs(neighbor.offset.x), Math.abs(neighbor.offset.y));
						if (y - radius >= 0 && y + radius < input.height) {
							Range xRange = new Range(Math.max(radius, validXRange.start), Math.min(input.width - radius, validXRange.end));
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
		return orientation;
	}
	static Range maskRange(BooleanMap mask, int y) {
		int first = -1;
		int last = -1;
		for (int x = 0; x < mask.width; ++x)
			if (mask.get(x, y)) {
				last = x;
				if (first < 0)
					first = x;
			}
		if (first >= 0)
			return new Range(first, last + 1);
		else
			return Range.zero;
	}
	static PointMap blockOrientations(PointMap orientation, BlockMap blocks, BooleanMap mask) {
		PointMap sums = new PointMap(blocks.blockCount);
		for (Cell block : blocks.blockCount) {
			if (mask.get(block)) {
				Block area = blocks.blockAreas.get(block);
				for (int y = area.bottom(); y < area.top(); ++y)
					for (int x = area.left(); x < area.right(); ++x)
						sums.add(block, orientation.get(x, y));
			}
		}
		return sums;
	}
	static PointMap smoothOrientation(PointMap orientation, BooleanMap mask) {
		final int radius = 1;
		Cell size = mask.size();
		PointMap smoothed = new PointMap(size);
		for (Cell block : size)
			if (mask.get(block)) {
				Block neighbors = Block.around(block, radius).intersect(new Block(size));
				for (int ny = neighbors.bottom(); ny < neighbors.top(); ++ny)
					for (int nx = neighbors.left(); nx < neighbors.right(); ++nx)
						if (mask.get(nx, ny))
							smoothed.add(block, orientation.get(nx, ny));
			}
		return smoothed;
	}
	static DoubleMap orientationAngles(PointMap vectors, BooleanMap mask) {
		Cell size = mask.size();
		DoubleMap angles = new DoubleMap(size);
		for (Cell block : size)
			if (mask.get(block))
				angles.set(block, Angle.atan(vectors.get(block)));
		return angles;
	}
	static class OrientedLineParams {
		int resolution = 32;
		int radius = 7;
		double step = 1.5;
		public OrientedLineParams resolution(int resolution) {
			this.resolution = resolution;
			return this;
		}
		public OrientedLineParams radius(int radius) {
			this.radius = radius;
			return this;
		}
		public OrientedLineParams step(double step) {
			this.step = step;
			return this;
		}
	}
	Cell[][] orientedLines(OrientedLineParams args) {
		Cell[][] result = new Cell[args.resolution][];
		for (int orientationIndex = 0; orientationIndex < args.resolution; ++orientationIndex) {
			List<Cell> line = new ArrayList<>();
			line.add(Cell.zero);
			Point direction = Angle.toVector(Angle.bucketCenter(orientationIndex, 2 * args.resolution));
			for (double r = args.radius; r >= 0.5; r /= args.step) {
				Cell sample = direction.multiply(r).round();
				if (!line.contains(sample)) {
					line.add(sample);
					line.add(sample.negate());
				}
			}
			result[orientationIndex] = line.toArray(new Cell[line.size()]);
		}
		return result;
	}
	static DoubleMap smoothRidges(DoubleMap input, DoubleMap orientation, BooleanMap mask, BlockMap blocks, double angle, Cell[][] lines) {
		DoubleMap output = new DoubleMap(input.size());
		for (Cell block : blocks.blockCount) {
			if (mask.get(block)) {
				Cell[] line = lines[Angle.quantize(Angle.add(orientation.get(block), angle), lines.length)];
				for (Cell linePoint : line) {
					Block target = blocks.blockAreas.get(block);
					Block source = target.move(linePoint).intersect(new Block(blocks.pixelCount));
					target = source.move(linePoint.negate());
					for (int y = target.bottom(); y < target.top(); ++y)
						for (int x = target.left(); x < target.right(); ++x)
							output.add(x, y, input.get(x + linePoint.x, y + linePoint.y));
				}
				Block blockArea = blocks.blockAreas.get(block);
				for (int y = blockArea.bottom(); y < blockArea.top(); ++y)
					for (int x = blockArea.left(); x < blockArea.right(); ++x)
						output.multiply(x, y, 1.0 / line.length);
			}
		}
		return output;
	}
	static BooleanMap binarize(DoubleMap input, DoubleMap baseline, BooleanMap mask, BlockMap blocks) {
		Cell size = input.size();
		BooleanMap binarized = new BooleanMap(size);
		for (Cell block : blocks.blockCount)
			if (mask.get(block)) {
				Block rect = blocks.blockAreas.get(block);
				for (int y = rect.bottom(); y < rect.top(); ++y)
					for (int x = rect.left(); x < rect.right(); ++x)
						if (input.get(x, y) - baseline.get(x, y) > 0)
							binarized.set(x, y, true);
			}
		return binarized;
	}
	static void cleanupBinarized(BooleanMap binary) {
		Cell size = binary.size();
		BooleanMap inverted = new BooleanMap(binary);
		inverted.invert();
		BooleanMap islands = filterBinarized(inverted);
		BooleanMap holes = filterBinarized(binary);
		for (int y = 0; y < size.y; ++y)
			for (int x = 0; x < size.x; ++x)
				binary.set(x, y, binary.get(x, y) && !islands.get(x, y) || holes.get(x, y));
		removeCrosses(binary);
	}
	static BooleanMap filterBinarized(BooleanMap input) {
		return vote(input, new VotingParameters().radius(2).majority(0.61).borderDist(17));
	}
	static void removeCrosses(BooleanMap input) {
		Cell size = input.size();
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
	static BooleanMap fillBlocks(BooleanMap mask, BlockMap blocks) {
		BooleanMap pixelized = new BooleanMap(blocks.pixelCount);
		for (Cell block : blocks.blockCount)
			if (mask.get(block))
				for (Cell pixel : blocks.blockAreas.get(block))
					pixelized.set(pixel, true);
		return pixelized;
	}
	static BooleanMap invert(BooleanMap binary, BooleanMap mask) {
		Cell size = binary.size();
		BooleanMap inverted = new BooleanMap(size);
		for (int y = 0; y < size.y; ++y)
			for (int x = 0; x < size.x; ++x)
				inverted.set(x, y, !binary.get(x, y) && mask.get(x, y));
		return inverted;
	}
}
