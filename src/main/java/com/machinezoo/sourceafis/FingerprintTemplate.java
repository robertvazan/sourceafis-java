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
 * Fingerprint template can be created from fingerprint image by calling {@link #FingerprintTemplate(byte[])}.
 * Since image processing is expensive, applications should cache serialized templates.
 * Serialization is performed by {@link #json()} and deserialization by {@link #FingerprintTemplate(String)}.
 * <p>
 * Matching is performed by constructing {@link FingerprintMatcher} and calling its {@link FingerprintMatcher#match(FingerprintTemplate)} method.
 * <p>
 * {@code FingerprintTemplate} contains search structures that speed up matching at the cost of some RAM.
 * These search structures do not contain any unique data. They can be recomputed from minutiae.
 * They are therefore excluded from serialized templates.
 * 
 * @see <a href="https://sourceafis.machinezoo.com/">SourceAFIS overview</a>
 * @see FingerprintMatcher
 */
public class FingerprintTemplate {
	private final FingerprintContext context = FingerprintContext.current();
	FingerprintMinutia[] minutiae = new FingerprintMinutia[0];
	NeighborEdge[][] edgeTable;
	/**
	 * Create fingerprint template from raw fingerprint image.
	 * Image must contain black fingerprint on white background at 500dpi.
	 * For images at different DPI, call {@link #FingerprintTemplate(byte[], double)}.
	 * <p>
	 * Aside from standard image formats supported by Java's {@link ImageIO}, for example JPEG, PNG, or BMP,
	 * this constructor also accepts ISO 19794-2 templates that carry fingerprint features (endings and bifurcations) without the original image.
	 * This feature may be removed in the future and developers are encouraged to collect and store raw fingerprint images.
	 * 
	 * @param image
	 *            fingerprint image in {@link ImageIO}-supported format or ISO 19794-2 file
	 * 
	 * @see #FingerprintTemplate(byte[], double)
	 */
	public FingerprintTemplate(byte[] image) {
		this(image, OptionalDouble.empty());
	}
	/**
	 * Create fingerprint template from raw fingerprint image with non-default DPI.
	 * This constructor's behavior is identical to {@link #FingerprintTemplate(byte[])}
	 * except that custom DPI (dots per inch) can be specified.
	 * Check your fingerprint reader specification for correct DPI value.
	 * 
	 * @param image
	 *            fingerprint image in {@link ImageIO}-supported format
	 * @param dpi
	 *            DPI of the image
	 * 
	 * @see #FingerprintTemplate(byte[])
	 */
	public FingerprintTemplate(byte[] image, double dpi) {
		this(image, OptionalDouble.of(dpi));
	}
	private FingerprintTemplate(byte[] image, OptionalDouble dpi) {
		context.log("extracting-features", null);
		if (isIso(image))
			minutiae = parseIso(image, dpi);
		else {
			context.log("image-dpi", dpi);
			context.log("serialized-image", image);
			DoubleMap raw = readImage(image);
			if (dpi.isPresent() && Math.abs(dpi.getAsDouble() - 500) > context.dpiTolerance)
				raw = scaleImage(raw, dpi.getAsDouble());
			BlockMap blocks = new BlockMap(raw.width, raw.height, context.blockSize);
			context.log("block-map", blocks);
			Histogram histogram = histogram(blocks, raw);
			Histogram smoothHistogram = smoothHistogram(blocks, histogram);
			BooleanMap mask = mask(blocks, histogram);
			DoubleMap equalized = equalize(blocks, raw, smoothHistogram, mask);
			DoubleMap orientation = orientationMap(equalized, mask, blocks);
			DoubleMap smoothed = smoothRidges(equalized, orientation, mask, blocks, 0, orientedLines(context.parallelSmoothinig));
			context.log("parallel-smoothing", smoothed);
			DoubleMap orthogonal = smoothRidges(smoothed, orientation, mask, blocks, Math.PI, orientedLines(context.orthogonalSmoothing));
			context.log("orthogonal-smoothing", orthogonal);
			BooleanMap binary = binarize(smoothed, orthogonal, mask, blocks);
			cleanupBinarized(binary);
			BooleanMap pixelMask = fillBlocks(mask, blocks);
			context.log("pixel-mask", pixelMask);
			BooleanMap inverted = invert(binary, pixelMask);
			context.log("masked-inverted", inverted);
			BooleanMap innerMask = innerMask(pixelMask);
			context.log("skeleton", "ridges");
			FingerprintSkeleton ridges = new FingerprintSkeleton(binary);
			context.log("skeleton", "valleys");
			FingerprintSkeleton valleys = new FingerprintSkeleton(inverted);
			collectMinutiae(ridges, MinutiaType.ENDING);
			collectMinutiae(valleys, MinutiaType.BIFURCATION);
			maskMinutiae(innerMask);
		}
		removeMinutiaClouds();
		limitTemplateSize();
		shuffleMinutiae();
		buildEdgeTable();
	}
	/**
	 * Deserialize fingerprint template from JSON string.
	 * This constructor reads JSON string produced by {@link #json()} to reconstruct exact copy of the original fingerprint template.
	 * Templates produced by previous versions of SourceAFIS may fail to deserialize correctly.
	 * Applications should re-extract all templates from original raw images when upgrading SourceAFIS.
	 * 
	 * @param json
	 *            serialized fingerprint template in JSON format
	 * 
	 * @see #json()
	 */
	public FingerprintTemplate(String json) {
		minutiae = new Gson().fromJson(json, FingerprintMinutia[].class);
		context.log("deserialized-minutiae", minutiae);
		buildEdgeTable();
	}
	/**
	 * Serialize fingerprint template to JSON string.
	 * Serialized template can be stored in database or sent over network.
	 * It can be deserialized by calling {@link #FingerprintTemplate(String)}.
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
	 * @see #FingerprintTemplate(String)
	 */
	public String json() {
		return new Gson().toJson(minutiae);
	}
	private static boolean isIso(byte[] iso) {
		return iso.length >= 30 && iso[0] == 'F' && iso[1] == 'M' && iso[2] == 'R' && iso[3] == 0;
	}
	private FingerprintMinutia[] parseIso(byte[] iso, OptionalDouble dpi) {
		try {
			DataInput in = new DataInputStream(new ByteArrayInputStream(iso));
			// 4B magic header "FMR\0"
			// 4B version " 20\0"
			// 4B template length in bytes (should be 28 + 6 * count + 2 + extra-data)
			// 2B junk
			in.skipBytes(14);
			// image size
			int width = in.readUnsignedShort();
			int height = in.readUnsignedShort();
			context.log("iso-size", new Cell(width, height));
			// pixels per cm X and Y, assuming 500dpi
			int xPixelsPerCM = in.readShort();
			int yPixelsPerCM = in.readShort();
			double dpiX = xPixelsPerCM * 255 / 100.0;
			double dpiY = yPixelsPerCM * 255 / 100.0;
			context.log("iso-dpi", new Point(dpiX, dpiY));
			if (dpi.isPresent()) {
				dpiX = dpi.getAsDouble();
				dpiY = dpi.getAsDouble();
			}
			// 1B number of fingerprints in the template (assuming 1)
			// 1B junk
			// 1B finger position
			// 1B junk
			// 1B fingerprint quality
			in.skipBytes(5);
			// minutia count
			int count = in.readUnsignedByte();
			List<FingerprintMinutia> minutiae = new ArrayList<>();
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
				if (Math.abs(dpiX - 500) > context.dpiTolerance)
					x = (int)Math.round(x / dpiX * 500);
				if (Math.abs(dpiY - 500) > context.dpiTolerance)
					y = (int)Math.round(y / dpiY * 500);
				FingerprintMinutia minutia = new FingerprintMinutia(
					new Cell(x, y),
					angle * Angle.PI2 / 256.0,
					type == 2 ? MinutiaType.BIFURCATION : MinutiaType.ENDING);
				minutiae.add(minutia);
			}
			// extra data length
			int extra = in.readUnsignedShort();
			// variable-length extra data section
			in.skipBytes(extra);
			FingerprintMinutia[] result = minutiae.stream().toArray(FingerprintMinutia[]::new);
			context.log("iso-minutiae", result);
			return result;
		} catch (IOException e) {
			throw new IllegalArgumentException("Invalid ISO 19794-2 template", e);
		}
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
		context.log("raw-image", map);
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
		context.log("scaled-image", output);
		return output;
	}
	Histogram histogram(BlockMap blocks, DoubleMap image) {
		Histogram histogram = new Histogram(blocks.blockCount.x, blocks.blockCount.y, context.histogramDepth);
		for (Cell block : blocks.blockCount) {
			Block area = blocks.blockAreas.get(block);
			for (int y = area.bottom(); y < area.top(); ++y)
				for (int x = area.left(); x < area.right(); ++x) {
					int depth = (int)(image.get(x, y) * histogram.depth);
					histogram.increment(block, histogram.constrain(depth));
				}
		}
		context.log("histogram", histogram);
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
		context.log("smooth-histogram", output);
		return output;
	}
	BooleanMap mask(BlockMap blocks, Histogram histogram) {
		DoubleMap contrast = clipContrast(blocks, histogram);
		BooleanMap mask = filterAbsoluteContrast(contrast);
		context.log("mask-stage", mask);
		mask.merge(filterRelativeContrast(contrast, blocks));
		context.log("mask-stage", mask);
		mask.merge(vote(mask, "contrast", context.contrastVote));
		context.log("mask-stage", mask);
		mask.merge(filterBlockErrors(mask));
		context.log("mask-stage", mask);
		mask.invert();
		context.log("mask-stage", mask);
		mask.merge(filterBlockErrors(mask));
		context.log("mask-stage", mask);
		mask.merge(filterBlockErrors(mask));
		context.log("mask-stage", mask);
		mask.merge(vote(mask, "mask", context.maskVote));
		context.log("final-mask", mask);
		return mask;
	}
	DoubleMap clipContrast(BlockMap blocks, Histogram histogram) {
		DoubleMap result = new DoubleMap(blocks.blockCount);
		for (Cell block : blocks.blockCount) {
			int volume = histogram.sum(block);
			int clipLimit = (int)Math.round(volume * context.clippedContrast);
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
		context.log("clipped-contrast", result);
		return result;
	}
	BooleanMap filterAbsoluteContrast(DoubleMap contrast) {
		BooleanMap result = new BooleanMap(contrast.size());
		for (Cell block : contrast.size())
			if (contrast.get(block) < context.minAbsoluteContrast)
				result.set(block, true);
		context.log("absolute-contrast", result);
		return result;
	}
	BooleanMap filterRelativeContrast(DoubleMap contrast, BlockMap blocks) {
		List<Double> sortedContrast = new ArrayList<>();
		for (Cell block : contrast.size())
			sortedContrast.add(contrast.get(block));
		sortedContrast.sort(Comparator.<Double>naturalOrder().reversed());
		int pixelsPerBlock = blocks.pixelCount.area() / blocks.blockCount.area();
		int sampleCount = Math.min(sortedContrast.size(), context.relativeContrastSample / pixelsPerBlock);
		int consideredBlocks = Math.max((int)Math.round(sampleCount * context.relativeContrastPercentile), 1);
		double averageContrast = sortedContrast.stream().mapToDouble(n -> n).limit(consideredBlocks).average().getAsDouble();
		double limit = averageContrast * context.minRelativeContrast;
		BooleanMap result = new BooleanMap(blocks.blockCount);
		for (Cell block : blocks.blockCount)
			if (contrast.get(block) < limit)
				result.set(block, true);
		context.log("relative-contrast", result);
		return result;
	}
	BooleanMap vote(BooleanMap input, String label, VotingParameters args) {
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
		context.log(label + "-vote", output);
		return output;
	}
	BooleanMap filterBlockErrors(BooleanMap input) {
		return vote(input, "block-errors", context.blockErrorsVote);
	}
	DoubleMap equalize(BlockMap blocks, DoubleMap image, Histogram histogram, BooleanMap blockMask) {
		final double rangeMin = -1;
		final double rangeMax = 1;
		final double rangeSize = rangeMax - rangeMin;
		final double widthMax = rangeSize / 256 * context.maxEqualizationScaling;
		final double widthMin = rangeSize / 256 * context.minEqualizationScaling;
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
		context.log("equalized", result);
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
		ConsideredOrientation[][] splits = new ConsideredOrientation[context.orientationSplit][];
		for (int i = 0; i < context.orientationSplit; ++i) {
			ConsideredOrientation[] orientations = splits[i] = new ConsideredOrientation[context.orientationsChecked];
			for (int j = 0; j < context.orientationsChecked; ++j) {
				ConsideredOrientation sample = orientations[j] = new ConsideredOrientation();
				do {
					double angle = random.nextDouble() * Math.PI;
					double distance = Doubles.interpolateExponential(context.minOrientationRadius, context.maxOrientationRadius, random.nextDouble());
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
		context.log("pixelwise-orientation", orientation);
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
		context.log("block-orientations", orientation);
		return sums;
	}
	PointMap smoothOrientation(PointMap orientation, BooleanMap mask) {
		Cell size = mask.size();
		PointMap smoothed = new PointMap(size);
		for (Cell block : size)
			if (mask.get(block)) {
				Block neighbors = Block.around(block, context.orientationSmoothingRadius).intersect(new Block(size));
				for (int ny = neighbors.bottom(); ny < neighbors.top(); ++ny)
					for (int nx = neighbors.left(); nx < neighbors.right(); ++nx)
						if (mask.get(nx, ny))
							smoothed.add(block, orientation.get(nx, ny));
			}
		context.log("smooth-orientations", smoothed);
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
		context.log("binarized", binarized);
		return binarized;
	}
	void cleanupBinarized(BooleanMap binary) {
		Cell size = binary.size();
		BooleanMap inverted = new BooleanMap(binary);
		inverted.invert();
		BooleanMap islands = vote(inverted, "islands", context.binarizedVote);
		BooleanMap holes = vote(binary, "holes", context.binarizedVote);
		for (int y = 0; y < size.y; ++y)
			for (int x = 0; x < size.x; ++x)
				binary.set(x, y, binary.get(x, y) && !islands.get(x, y) || holes.get(x, y));
		removeCrosses(binary);
		context.log("clean-binarized", binary);
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
		if (context.innerMaskBorderDistance >= 1)
			inner = shrinkMask(inner, 1);
		int total = 1;
		for (int step = 1; total + step <= context.innerMaskBorderDistance; step *= 2) {
			inner = shrinkMask(inner, step);
			total += step;
		}
		if (total < context.innerMaskBorderDistance)
			inner = shrinkMask(inner, context.innerMaskBorderDistance - total);
		context.log("inner-mask", inner);
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
				.map(m -> new FingerprintMinutia(m.position, m.ridges.get(0).direction(context), type)))
			.toArray(FingerprintMinutia[]::new);
		context.log("collected-minutiae", minutiae);
	}
	void maskMinutiae(BooleanMap mask) {
		minutiae = Arrays.stream(minutiae)
			.filter(minutia -> {
				Cell arrow = Angle.toVector(minutia.direction).multiply(-context.maskDisplacement).round();
				return mask.get(minutia.position.plus(arrow), false);
			})
			.toArray(FingerprintMinutia[]::new);
		context.log("masked-minutiae", minutiae);
	}
	void removeMinutiaClouds() {
		int radiusSq = Integers.sq(context.minutiaCloudRadius);
		Set<FingerprintMinutia> removed = Arrays.stream(minutiae)
			.filter(minutia -> context.maxCloudSize < Arrays.stream(minutiae)
				.filter(neighbor -> neighbor.position.minus(minutia.position).lengthSq() <= radiusSq)
				.count() - 1)
			.collect(toSet());
		minutiae = Arrays.stream(minutiae)
			.filter(minutia -> !removed.contains(minutia))
			.toArray(FingerprintMinutia[]::new);
		context.log("removed-minutia-clouds", minutiae);
	}
	void limitTemplateSize() {
		if (minutiae.length > context.maxMinutiae) {
			minutiae = Arrays.stream(minutiae)
				.sorted(Comparator.<FingerprintMinutia>comparingInt(
					minutia -> Arrays.stream(minutiae)
						.mapToInt(neighbor -> minutia.position.minus(neighbor.position).lengthSq())
						.sorted()
						.skip(context.sortByNeighbor)
						.findFirst().orElse(Integer.MAX_VALUE))
					.reversed())
				.limit(context.maxMinutiae)
				.toArray(FingerprintMinutia[]::new);
		}
		context.log("template-size-limit", minutiae);
	}
	void shuffleMinutiae() {
		int seed = 0;
		for (FingerprintMinutia minutia : minutiae)
			seed += minutia.direction + minutia.position.x + minutia.position.y + minutia.type.ordinal();
		Collections.shuffle(Arrays.asList(minutiae), new Random(seed));
		context.log("shuffled-minutiae", minutiae);
	}
	void buildEdgeTable() {
		edgeTable = new NeighborEdge[minutiae.length][];
		List<NeighborEdge> edges = new ArrayList<>();
		int[] allSqDistances = new int[minutiae.length];
		for (int reference = 0; reference < edgeTable.length; ++reference) {
			Cell referencePosition = minutiae[reference].position;
			int sqMaxDistance = Integers.sq(context.edgeTableRange);
			if (minutiae.length - 1 > context.edgeTableNeighbors) {
				for (int neighbor = 0; neighbor < minutiae.length; ++neighbor)
					allSqDistances[neighbor] = referencePosition.minus(minutiae[neighbor].position).lengthSq();
				Arrays.sort(allSqDistances);
				sqMaxDistance = allSqDistances[context.edgeTableNeighbors];
			}
			for (int neighbor = 0; neighbor < minutiae.length; ++neighbor) {
				if (neighbor != reference && referencePosition.minus(minutiae[neighbor].position).lengthSq() <= sqMaxDistance)
					edges.add(new NeighborEdge(minutiae, reference, neighbor));
			}
			edges.sort(Comparator.<NeighborEdge>comparingInt(e -> e.length).thenComparingInt(e -> e.neighbor));
			while (edges.size() > context.edgeTableNeighbors)
				edges.remove(edges.size() - 1);
			edgeTable[reference] = edges.toArray(new NeighborEdge[edges.size()]);
			edges.clear();
		}
		context.log("edge-table", edgeTable);
	}
}
