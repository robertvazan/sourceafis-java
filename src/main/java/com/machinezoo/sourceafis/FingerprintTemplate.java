// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;
import javax.imageio.*;
import com.google.gson.*;
import com.machinezoo.sourceafis.models.*;
import lombok.*;

/**
 * Biometric description of a fingerprint suitable for efficient matching.
 * Fingerprint template holds high-level fingerprint features, specifically ridge endings and bifurcations (minutiae).
 * Original image is not preserved in the fingerprint template and there is no way to reconstruct the original fingerprint from its template.
 * <p>
 * Fingerprint template can be created from fingerprint image by calling {@link #FingerprintTemplate(byte[], double)}.
 * Since image processing is expensive, applications should cache serialized templates.
 * Serialization is performed by {@link #toJson()} and deserialization by {@link #fromJson(String)}.
 * <p>
 * Matching is performed by constructing {@link FingerprintMatcher} and calling its {@link FingerprintMatcher#match(FingerprintTemplate)} method.
 * <p>
 * {@code FingerprintTemplate} contains two kinds of data: fingerprint features and search data structures.
 * Search data structures speed up matching at the cost of some RAM.
 * Only fingerprint features are serialized. Search data structures are recomputed after every deserialization.
 * 
 * @see <a href="https://sourceafis.machinezoo.com/">SourceAFIS overview</a>
 * @see FingerprintMatcher
 */
public class FingerprintTemplate {
	private final DataLogger logger = DataLogger.current();
	FingerprintMinutia[] minutiae = new FingerprintMinutia[0];
	NeighborEdge[][] edgeTable;
	/**
	 * Create fingerprint template from raw fingerprint image.
	 * Image must contain black fingerprint on white background with the specified DPI (dots per inch).
	 * Check your fingerprint reader specification for correct DPI value.
	 * All image formats supported by Java's {@link ImageIO} are accepted, for example JPEG, PNG, or BMP,
	 * 
	 * @param image
	 *            fingerprint image in {@link ImageIO}-supported format
	 * @param dpi
	 *            DPI of the image, usually around 500
	 * 
	 * @see #FingerprintTemplate(byte[])
	 */
	public FingerprintTemplate(byte[] image, double dpi) {
		logger.log("extracting-features", null);
		logger.log("image-dpi", dpi);
		logger.log("serialized-image", image);
		DoubleMap raw = readImage(image);
		if (Math.abs(dpi - 500) > Parameters.dpiTolerance)
			raw = scaleImage(raw, dpi);
		BlockMap blocks = new BlockMap(raw.width, raw.height, Parameters.blockSize);
		logger.log("block-map", blocks);
		Histogram histogram = histogram(blocks, raw);
		Histogram smoothHistogram = smoothHistogram(blocks, histogram);
		BooleanMap mask = mask(blocks, histogram);
		DoubleMap equalized = equalize(blocks, raw, smoothHistogram, mask);
		DoubleMap orientation = orientationMap(equalized, mask, blocks);
		DoubleMap smoothed = smoothRidges(equalized, orientation, mask, blocks, 0,
			orientedLines(Parameters.parallelSmoothinigResolution, Parameters.parallelSmoothinigRadius, Parameters.parallelSmoothinigStep));
		logger.log("parallel-smoothing", smoothed);
		DoubleMap orthogonal = smoothRidges(smoothed, orientation, mask, blocks, Math.PI,
			orientedLines(Parameters.orthogonalSmoothinigResolution, Parameters.orthogonalSmoothinigRadius, Parameters.orthogonalSmoothinigStep));
		logger.log("orthogonal-smoothing", orthogonal);
		BooleanMap binary = binarize(smoothed, orthogonal, mask, blocks);
		cleanupBinarized(binary);
		BooleanMap pixelMask = fillBlocks(mask, blocks);
		logger.log("pixel-mask", pixelMask);
		BooleanMap inverted = invert(binary, pixelMask);
		logger.log("masked-inverted", inverted);
		BooleanMap innerMask = innerMask(pixelMask);
		logger.log("skeleton", "ridges");
		FingerprintSkeleton ridges = new FingerprintSkeleton(binary);
		logger.log("skeleton", "valleys");
		FingerprintSkeleton valleys = new FingerprintSkeleton(inverted);
		collectMinutiae(ridges, MinutiaType.ENDING);
		collectMinutiae(valleys, MinutiaType.BIFURCATION);
		maskMinutiae(innerMask);
		removeMinutiaClouds();
		limitTemplateSize();
		shuffleMinutiae();
		buildEdgeTable();
	}
	/**
	 * Deserialize fingerprint template from JSON string.
	 * This constructor reads JSON string produced by {@link #toJson()} to reconstruct exact copy of the original fingerprint template.
	 * Templates produced by previous versions of SourceAFIS may fail to deserialize correctly.
	 * Applications should re-extract all templates from original raw images when upgrading SourceAFIS.
	 * 
	 * @param json
	 *            serialized fingerprint template in JSON format produced by {@link #toJson()}
	 * @return deserialized fingerprint template
	 * 
	 * @see #toJson()
	 */
	public static FingerprintTemplate fromJson(String json) {
		return new FingerprintTemplate(json);
	}
	private FingerprintTemplate(String json) {
		minutiae = new Gson().fromJson(json, FingerprintMinutia[].class);
		logger.log("deserialized-minutiae", minutiae);
		buildEdgeTable();
	}
	/**
	 * Serialize fingerprint template to JSON string.
	 * Serialized template can be stored in database or sent over network.
	 * It can be deserialized by calling {@link #fromJson(String)}.
	 * Persisting templates alongside fingerprint images allows applications to start faster,
	 * because template deserialization is more than 100x faster than re-extraction from raw image.
	 * <p>
	 * Serialized template excludes search structures that {@code FingerprintTemplate} keeps to speed up matching.
	 * Serialized template is therefore much smaller than in-memory {@code FingerprintTemplate}.
	 * <p>
	 * Serialization format can change with every SourceAFIS version. There is no backward compatibility of templates.
	 * Applications should preserve raw fingerprint images, so that templates can be re-extracted after SourceAFIS upgrade.
	 * 
	 * @return serialized fingerprint template in JSON format
	 * 
	 * @see #fromJson(String)
	 */
	public String toJson() {
		return new Gson().toJson(minutiae);
	}
	/**
	 * Import ISO 19794-2 fingerprint template from another fingerprint recognition system.
	 * This method can import biometric data from ISO 19794-2 templates,
	 * which carry fingerprint features (endings and bifurcations) without the original image.
	 * <p>
	 * This method is written for ISO 19794-2:2005, but it should be able to handle ISO 19794-2:2011 templates.
	 * If you believe you have a conforming template, but this method doesn't accept it, mail the template in for analysis.
	 * No other fingerprint template formats are currently supported.
	 * <p>
	 * Note that the use of ISO 19794-2 templates is strongly discouraged
	 * and support for the format might be removed in future releases.
	 * This is because ISO is very unfriendly to opensource developers,
	 * Its "standards" are only available for a high fee and with no redistribution rights.
	 * There is only one truly open and widely used fingerprint exchange format: fingerprint images.
	 * Application developers are encouraged to collect, store, and transfer fingerprints as raw images.
	 * Besides compatibility and simplicity this brings,
	 * use of raw images allows SourceAFIS to co-tune its feature extractor and matcher for higher accuracy.
	 * 
	 * @param iso ISO 19794-2 template to import
	 * @return converted fingerprint template
	 * 
	 * @see #FingerprintTemplate(byte[], double)
	 * @see #fromJson(String)
	 * @see #toJson()
	 */
	public static FingerprintTemplate convert(byte[] iso) {
		return new FingerprintTemplate(iso);
	}
	private FingerprintTemplate(byte[] iso) {
		if (iso.length < 30)
			throw new IllegalArgumentException("Array too small to be an ISO 19794-2 template");
		try {
			DataInput in = new DataInputStream(new ByteArrayInputStream(iso));
			// 4B magic header "FMR\0"
			if (in.readByte() != 'F' || in.readByte() != 'M' || in.readByte() != 'R' || in.readByte() != 0)
				throw new IllegalArgumentException("This is not an ISO 19794-2 template");
			// 4B version " 20\0"
			// 4B template length in bytes (should be 28 + 6 * count + 2 + extra-data)
			// 2B junk
			in.skipBytes(10);
			// image size
			int width = in.readUnsignedShort();
			int height = in.readUnsignedShort();
			logger.log("iso-size", new Cell(width, height));
			// pixels per cm X and Y, assuming 500dpi
			int xPixelsPerCM = in.readShort();
			int yPixelsPerCM = in.readShort();
			double dpiX = xPixelsPerCM * 255 / 100.0;
			double dpiY = yPixelsPerCM * 255 / 100.0;
			logger.log("iso-dpi", new Point(dpiX, dpiY));
			// 1B number of fingerprints in the template (assuming 1)
			// 1B junk
			// 1B finger position
			// 1B junk
			// 1B fingerprint quality
			in.skipBytes(5);
			// minutia count
			int count = in.readUnsignedByte();
			List<FingerprintMinutia> list = new ArrayList<>();
			for (int i = 0; i < count; ++i) {
				// X position, upper two bits are type
				int packedX = in.readUnsignedShort();
				// Y position, upper two bits ignored
				int packedY = in.readUnsignedShort();
				// angle, 0..255 equivalent to 0..2pi
				int angle = in.readUnsignedByte();
				// 1B minutia quality
				in.skipBytes(1);
				// type: 01 ending, 10 bifurcation, 00 other (treated as ending)
				int type = (packedX >> 14) & 0x3;
				int x = packedX & 0x3fff;
				int y = packedY & 0x3fff;
				if (Math.abs(dpiX - 500) > Parameters.dpiTolerance)
					x = (int)Math.round(x / dpiX * 500);
				if (Math.abs(dpiY - 500) > Parameters.dpiTolerance)
					y = (int)Math.round(y / dpiY * 500);
				FingerprintMinutia minutia = new FingerprintMinutia(
					new Cell(x, y),
					angle * Angle.PI2 / 256.0,
					type == 2 ? MinutiaType.BIFURCATION : MinutiaType.ENDING);
				list.add(minutia);
			}
			// extra data length
			int extra = in.readUnsignedShort();
			// variable-length extra data section
			in.skipBytes(extra);
			minutiae = list.stream().toArray(FingerprintMinutia[]::new);
			logger.log("iso-minutiae", minutiae);
		} catch (IOException e) {
			throw new IllegalArgumentException("Invalid ISO 19794-2 template", e);
		}
		shuffleMinutiae();
		buildEdgeTable();
	}
	@SneakyThrows DoubleMap readImage(byte[] serialized) {
		BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(serialized));
		if (buffered == null)
			throw new IllegalArgumentException("Unsupported image format");
		int width = buffered.getWidth();
		int height = buffered.getHeight();
		int[] pixels = new int[width * height];
		buffered.getRGB(0, 0, width, height, pixels, 0, width);
		DoubleMap map = new DoubleMap(width, height);
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				int pixel = pixels[y * width + x];
				int color = (pixel & 0xff) + ((pixel >> 8) & 0xff) + ((pixel >> 16) & 0xff);
				map.set(x, height - y - 1, 1 - color * (1.0 / (3.0 * 255.0)));
			}
		}
		logger.log("raw-image", map);
		return map;
	}
	DoubleMap scaleImage(DoubleMap input, double dpi) {
		return scaleImage(input, (int)Math.round(500.0 / dpi * input.width), (int)Math.round(500.0 / dpi * input.height));
	}
	DoubleMap scaleImage(DoubleMap input, int newWidth, int newHeight) {
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
		logger.log("scaled-image", output);
		return output;
	}
	Histogram histogram(BlockMap blocks, DoubleMap image) {
		Histogram histogram = new Histogram(blocks.blockCount.x, blocks.blockCount.y, Parameters.histogramDepth);
		for (Cell block : blocks.blockCount) {
			Block area = blocks.blockAreas.get(block);
			for (int y = area.bottom(); y < area.top(); ++y)
				for (int x = area.left(); x < area.right(); ++x) {
					int depth = (int)(image.get(x, y) * histogram.depth);
					histogram.increment(block, histogram.constrain(depth));
				}
		}
		logger.log("histogram", histogram);
		return histogram;
	}
	Histogram smoothHistogram(BlockMap blocks, Histogram input) {
		Cell[] blocksAround = new Cell[] { new Cell(0, 0), new Cell(-1, 0), new Cell(0, -1), new Cell(-1, -1) };
		Histogram output = new Histogram(blocks.cornerCount.x, blocks.cornerCount.y, input.depth);
		for (Cell corner : blocks.cornerCount) {
			for (Cell relative : blocksAround) {
				Cell block = corner.plus(relative);
				if (blocks.blockCount.contains(block)) {
					for (int i = 0; i < input.depth; ++i)
						output.add(corner, i, input.get(block, i));
				}
			}
		}
		logger.log("smooth-histogram", output);
		return output;
	}
	BooleanMap mask(BlockMap blocks, Histogram histogram) {
		DoubleMap contrast = clipContrast(blocks, histogram);
		BooleanMap mask = filterAbsoluteContrast(contrast);
		logger.log("mask-stage", mask);
		mask.merge(filterRelativeContrast(contrast, blocks));
		logger.log("mask-stage", mask);
		mask.merge(vote(mask, "contrast", Parameters.contrastVoteRadius, Parameters.contrastVoteMajority, Parameters.contrastVoteBorderDistance));
		logger.log("mask-stage", mask);
		mask.merge(filterBlockErrors(mask));
		logger.log("mask-stage", mask);
		mask.invert();
		logger.log("mask-stage", mask);
		mask.merge(filterBlockErrors(mask));
		logger.log("mask-stage", mask);
		mask.merge(filterBlockErrors(mask));
		logger.log("mask-stage", mask);
		mask.merge(vote(mask, "mask", Parameters.maskVoteRadius, Parameters.maskVoteMajority, Parameters.maskVoteBorderDistance));
		logger.log("final-mask", mask);
		return mask;
	}
	DoubleMap clipContrast(BlockMap blocks, Histogram histogram) {
		DoubleMap result = new DoubleMap(blocks.blockCount);
		for (Cell block : blocks.blockCount) {
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
		logger.log("clipped-contrast", result);
		return result;
	}
	BooleanMap filterAbsoluteContrast(DoubleMap contrast) {
		BooleanMap result = new BooleanMap(contrast.size());
		for (Cell block : contrast.size())
			if (contrast.get(block) < Parameters.minAbsoluteContrast)
				result.set(block, true);
		logger.log("absolute-contrast", result);
		return result;
	}
	BooleanMap filterRelativeContrast(DoubleMap contrast, BlockMap blocks) {
		List<Double> sortedContrast = new ArrayList<>();
		for (Cell block : contrast.size())
			sortedContrast.add(contrast.get(block));
		sortedContrast.sort(Comparator.<Double>naturalOrder().reversed());
		int pixelsPerBlock = blocks.pixelCount.area() / blocks.blockCount.area();
		int sampleCount = Math.min(sortedContrast.size(), Parameters.relativeContrastSample / pixelsPerBlock);
		int consideredBlocks = Math.max((int)Math.round(sampleCount * Parameters.relativeContrastPercentile), 1);
		double averageContrast = sortedContrast.stream().mapToDouble(n -> n).limit(consideredBlocks).average().getAsDouble();
		double limit = averageContrast * Parameters.minRelativeContrast;
		BooleanMap result = new BooleanMap(blocks.blockCount);
		for (Cell block : blocks.blockCount)
			if (contrast.get(block) < limit)
				result.set(block, true);
		logger.log("relative-contrast", result);
		return result;
	}
	BooleanMap vote(BooleanMap input, String label, int radius, double majority, int borderDistance) {
		Cell size = input.size();
		Block rect = new Block(borderDistance, borderDistance, size.x - 2 * borderDistance, size.y - 2 * borderDistance);
		BooleanMap output = new BooleanMap(size);
		for (Cell center : rect) {
			Block neighborhood = Block.around(center, radius).intersect(new Block(size));
			int ones = 0;
			for (int ny = neighborhood.bottom(); ny < neighborhood.top(); ++ny)
				for (int nx = neighborhood.left(); nx < neighborhood.right(); ++nx)
					if (input.get(nx, ny))
						++ones;
			double voteWeight = 1.0 / neighborhood.area();
			if (ones * voteWeight >= majority)
				output.set(center, true);
		}
		logger.log(label + "-vote", output);
		return output;
	}
	BooleanMap filterBlockErrors(BooleanMap input) {
		return vote(input, "block-errors", Parameters.blockErrorsVoteRadius, Parameters.blockErrorsVoteMajority, Parameters.blockErrorsVoteBorderDistance);
	}
	DoubleMap equalize(BlockMap blocks, DoubleMap image, Histogram histogram, BooleanMap blockMask) {
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
		logger.log("equalized", result);
		return result;
	}
	DoubleMap orientationMap(DoubleMap image, BooleanMap mask, BlockMap blocks) {
		PointMap accumulated = pixelwiseOrientation(image, mask, blocks);
		PointMap byBlock = blockOrientations(accumulated, blocks, mask);
		PointMap smooth = smoothOrientation(byBlock, mask);
		return orientationAngles(smooth, mask);
	}
	static class ConsideredOrientation {
		Cell offset;
		Point orientation;
	}
	ConsideredOrientation[][] planOrientations() {
		Random random = new Random(0);
		ConsideredOrientation[][] splits = new ConsideredOrientation[Parameters.orientationSplit][];
		for (int i = 0; i < Parameters.orientationSplit; ++i) {
			ConsideredOrientation[] orientations = splits[i] = new ConsideredOrientation[Parameters.orientationsChecked];
			for (int j = 0; j < Parameters.orientationsChecked; ++j) {
				ConsideredOrientation sample = orientations[j] = new ConsideredOrientation();
				do {
					double angle = random.nextDouble() * Math.PI;
					double distance = Doubles.interpolateExponential(Parameters.minOrientationRadius, Parameters.maxOrientationRadius, random.nextDouble());
					sample.offset = Angle.toVector(angle).multiply(distance).round();
				} while (sample.offset.equals(Cell.zero) || sample.offset.y < 0 || Arrays.stream(orientations).limit(j).anyMatch(o -> o.offset.equals(sample.offset)));
				sample.orientation = Angle.toVector(Angle.add(Angle.toOrientation(Angle.atan(sample.offset.toPoint())), Math.PI));
			}
		}
		return splits;
	}
	PointMap pixelwiseOrientation(DoubleMap input, BooleanMap mask, BlockMap blocks) {
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
		logger.log("pixelwise-orientation", orientation);
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
	PointMap blockOrientations(PointMap orientation, BlockMap blocks, BooleanMap mask) {
		PointMap sums = new PointMap(blocks.blockCount);
		for (Cell block : blocks.blockCount) {
			if (mask.get(block)) {
				Block area = blocks.blockAreas.get(block);
				for (int y = area.bottom(); y < area.top(); ++y)
					for (int x = area.left(); x < area.right(); ++x)
						sums.add(block, orientation.get(x, y));
			}
		}
		logger.log("block-orientations", orientation);
		return sums;
	}
	PointMap smoothOrientation(PointMap orientation, BooleanMap mask) {
		Cell size = mask.size();
		PointMap smoothed = new PointMap(size);
		for (Cell block : size)
			if (mask.get(block)) {
				Block neighbors = Block.around(block, Parameters.orientationSmoothingRadius).intersect(new Block(size));
				for (int ny = neighbors.bottom(); ny < neighbors.top(); ++ny)
					for (int nx = neighbors.left(); nx < neighbors.right(); ++nx)
						if (mask.get(nx, ny))
							smoothed.add(block, orientation.get(nx, ny));
			}
		logger.log("smooth-orientations", smoothed);
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
	Cell[][] orientedLines(int resolution, int radius, double step) {
		Cell[][] result = new Cell[resolution][];
		for (int orientationIndex = 0; orientationIndex < resolution; ++orientationIndex) {
			List<Cell> line = new ArrayList<>();
			line.add(Cell.zero);
			Point direction = Angle.toVector(Angle.bucketCenter(orientationIndex, 2 * resolution));
			for (double r = radius; r >= 0.5; r /= step) {
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
	BooleanMap binarize(DoubleMap input, DoubleMap baseline, BooleanMap mask, BlockMap blocks) {
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
		logger.log("binarized", binarized);
		return binarized;
	}
	void cleanupBinarized(BooleanMap binary) {
		Cell size = binary.size();
		BooleanMap inverted = new BooleanMap(binary);
		inverted.invert();
		BooleanMap islands = vote(inverted, "islands", Parameters.binarizedVoteRadius, Parameters.binarizedVoteMajority, Parameters.binarizedVoteBorderDistance);
		BooleanMap holes = vote(binary, "holes", Parameters.binarizedVoteRadius, Parameters.binarizedVoteMajority, Parameters.binarizedVoteBorderDistance);
		for (int y = 0; y < size.y; ++y)
			for (int x = 0; x < size.x; ++x)
				binary.set(x, y, binary.get(x, y) && !islands.get(x, y) || holes.get(x, y));
		removeCrosses(binary);
		logger.log("clean-binarized", binary);
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
	BooleanMap innerMask(BooleanMap outer) {
		Cell size = outer.size();
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
		logger.log("inner-mask", inner);
		return inner;
	}
	static BooleanMap shrinkMask(BooleanMap mask, int amount) {
		Cell size = mask.size();
		BooleanMap shrunk = new BooleanMap(size);
		for (int y = amount; y < size.y - amount; ++y)
			for (int x = amount; x < size.x - amount; ++x)
				shrunk.set(x, y, mask.get(x, y - amount) && mask.get(x, y + amount) && mask.get(x - amount, y) && mask.get(x + amount, y));
		return shrunk;
	}
	void collectMinutiae(FingerprintSkeleton skeleton, MinutiaType type) {
		minutiae = Stream.concat(
			Arrays.stream(minutiae),
			skeleton.minutiae.stream()
				.filter(m -> m.considered && m.ridges.size() == 1)
				.map(m -> new FingerprintMinutia(m.position, m.ridges.get(0).direction(), type)))
			.toArray(FingerprintMinutia[]::new);
		logger.log("collected-minutiae", minutiae);
	}
	void maskMinutiae(BooleanMap mask) {
		minutiae = Arrays.stream(minutiae)
			.filter(minutia -> {
				Cell arrow = Angle.toVector(minutia.direction).multiply(-Parameters.maskDisplacement).round();
				return mask.get(minutia.position.plus(arrow), false);
			})
			.toArray(FingerprintMinutia[]::new);
		logger.log("masked-minutiae", minutiae);
	}
	void removeMinutiaClouds() {
		int radiusSq = Integers.sq(Parameters.minutiaCloudRadius);
		Set<FingerprintMinutia> removed = Arrays.stream(minutiae)
			.filter(minutia -> Parameters.maxCloudSize < Arrays.stream(minutiae)
				.filter(neighbor -> neighbor.position.minus(minutia.position).lengthSq() <= radiusSq)
				.count() - 1)
			.collect(toSet());
		minutiae = Arrays.stream(minutiae)
			.filter(minutia -> !removed.contains(minutia))
			.toArray(FingerprintMinutia[]::new);
		logger.log("removed-minutia-clouds", minutiae);
	}
	void limitTemplateSize() {
		if (minutiae.length > Parameters.maxMinutiae) {
			minutiae = Arrays.stream(minutiae)
				.sorted(Comparator.<FingerprintMinutia>comparingInt(
					minutia -> Arrays.stream(minutiae)
						.mapToInt(neighbor -> minutia.position.minus(neighbor.position).lengthSq())
						.sorted()
						.skip(Parameters.sortByNeighbor)
						.findFirst().orElse(Integer.MAX_VALUE))
					.reversed())
				.limit(Parameters.maxMinutiae)
				.toArray(FingerprintMinutia[]::new);
		}
		logger.log("template-size-limit", minutiae);
	}
	void shuffleMinutiae() {
		int seed = 0;
		for (FingerprintMinutia minutia : minutiae)
			seed += minutia.direction + minutia.position.x + minutia.position.y + minutia.type.ordinal();
		Collections.shuffle(Arrays.asList(minutiae), new Random(seed));
		logger.log("shuffled-minutiae", minutiae);
	}
	void buildEdgeTable() {
		edgeTable = new NeighborEdge[minutiae.length][];
		List<NeighborEdge> edges = new ArrayList<>();
		int[] allSqDistances = new int[minutiae.length];
		for (int reference = 0; reference < edgeTable.length; ++reference) {
			Cell referencePosition = minutiae[reference].position;
			int sqMaxDistance = Integers.sq(Parameters.edgeTableRange);
			if (minutiae.length - 1 > Parameters.edgeTableNeighbors) {
				for (int neighbor = 0; neighbor < minutiae.length; ++neighbor)
					allSqDistances[neighbor] = referencePosition.minus(minutiae[neighbor].position).lengthSq();
				Arrays.sort(allSqDistances);
				sqMaxDistance = allSqDistances[Parameters.edgeTableNeighbors];
			}
			for (int neighbor = 0; neighbor < minutiae.length; ++neighbor) {
				if (neighbor != reference && referencePosition.minus(minutiae[neighbor].position).lengthSq() <= sqMaxDistance)
					edges.add(new NeighborEdge(minutiae, reference, neighbor));
			}
			edges.sort(Comparator.<NeighborEdge>comparingInt(e -> e.length).thenComparingInt(e -> e.neighbor));
			while (edges.size() > Parameters.edgeTableNeighbors)
				edges.remove(edges.size() - 1);
			edgeTable[reference] = edges.toArray(new NeighborEdge[edges.size()]);
			edges.clear();
		}
		logger.log("edge-table", edgeTable);
	}
}
