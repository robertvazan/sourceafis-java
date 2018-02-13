// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;
import java.util.zip.*;
import com.google.gson.*;
import gnu.trove.map.hash.*;
import lombok.*;

public abstract class FingerprintTransparency implements AutoCloseable {
	private static final ThreadLocal<FingerprintTransparency> current = new ThreadLocal<>();
	private FingerprintTransparency outer;
	private boolean closed;
	private List<PairingEdge> supportingEdges = new ArrayList<>();
	static final FingerprintTransparency none = new FingerprintTransparency() {
		@Override protected void log(String name, Map<String, InputStream> data) {
		}
	};
	protected abstract void log(String name, Map<String, InputStream> data);
	protected FingerprintTransparency() {
		outer = current.get();
		current.set(this);
	}
	@Override public void close() {
		if (!closed) {
			closed = true;
			current.set(outer);
			outer = null;
		}
	}
	static FingerprintTransparency current() {
		return Optional.ofNullable(current.get()).orElse(none);
	}
	public static FingerprintTransparency zip(ZipOutputStream zip) {
		byte[] buffer = new byte[4096];
		return new FingerprintTransparency() {
			int offset;
			@Override @SneakyThrows protected void log(String name, Map<String, InputStream> data) {
				++offset;
				for (String suffix : data.keySet()) {
					zip.putNextEntry(new ZipEntry(String.format("%02d", offset) + "-" + name + suffix));
					InputStream stream = data.get(suffix);
					while (true) {
						int read = stream.read(buffer);
						if (read <= 0)
							break;
						zip.write(buffer, 0, read);
					}
					zip.closeEntry();
				}
			}
			@Override @SneakyThrows public void close() {
				super.close();
				zip.close();
			}
		};
	}
	boolean logging() {
		return this != none;
	}
	void logImageDecoded(DoubleMap image) {
		logDoubleMap("image-decoded", image);
	}
	void logImageScaled(DoubleMap image) {
		logDoubleMap("image-scaled", image);
	}
	void logBlockMap(BlockMap blocks) {
		Supplier<Object> source = () -> new JsonBlockMap(blocks);
		log("block-map", ".json", streamJson(source));
	}
	@SuppressWarnings("unused") private static class JsonBlockMap {
		Cell pixelCount;
		Cell blockCount;
		Cell cornerCount;
		JsonGrid corners;
		JsonGrid centers;
		JsonGrid cornerAreas;
		JsonBlockMap(BlockMap blocks) {
			pixelCount = blocks.pixelCount;
			blockCount = blocks.blockCount;
			cornerCount = blocks.cornerCount;
			corners = new JsonGrid(blocks.corners);
			centers = new JsonGrid(blocks.blockCenters);
			cornerAreas = new JsonGrid(blocks.cornerAreas.corners);
		}
	}
	@SuppressWarnings("unused") private static class JsonGrid {
		int[] x;
		int[] y;
		JsonGrid(CellGrid grid) {
			x = grid.allX;
			y = grid.allY;
		}
	}
	void logBlockHistogram(Histogram histogram) {
		logHistogram("histogram", histogram);
	}
	void logSmoothedHistogram(Histogram histogram) {
		logHistogram("histogram-smoothed", histogram);
	}
	void logContrastClipped(DoubleMap contrast) {
		logDoubleMap("contrast-clipped", contrast);
	}
	void logContrastAbsolute(BooleanMap mask) {
		logBooleanMap("contrast-absolute", mask);
	}
	void logContrastRelative(BooleanMap mask) {
		logBooleanMap("contrast-relative", mask);
	}
	void logContrastCombined(BooleanMap mask) {
		logBooleanMap("contrast-combined", mask);
	}
	void logContrastFiltered(BooleanMap mask) {
		logBooleanMap("contrast-filtered", mask);
	}
	void logEqualized(DoubleMap image) {
		logDoubleMap("equalized", image);
	}
	void logOrientationPixelwise(PointMap orientations) {
		logPointMap("orientation-pixelwise", orientations);
	}
	void logOrientationBlocks(PointMap orientations) {
		logPointMap("orientation-blocks", orientations);
	}
	void logOrientationSmoothed(PointMap orientations) {
		logPointMap("orientation-smoothed", orientations);
	}
	void logParallelSmoothing(DoubleMap smoothed) {
		logDoubleMap("parallel-smoothing", smoothed);
	}
	void logOrthogonalSmoothing(DoubleMap smoothed) {
		logDoubleMap("orthogonal-smoothing", smoothed);
	}
	void logBinarized(BooleanMap image) {
		logBooleanMap("binarized", image);
	}
	void logBinarizedFiltered(BooleanMap image) {
		logBooleanMap("binarized-filtered", image);
	}
	void logPixelMask(BooleanMap image) {
		logBooleanMap("pixel-mask", image);
	}
	void logInnerMask(BooleanMap image) {
		logBooleanMap("inner-mask", image);
	}
	void logSkeletonBinarized(SkeletonType type, BooleanMap image) {
		logBooleanMap(type.prefix + "binarized", image);
	}
	void logThinned(SkeletonType type, BooleanMap image) {
		logBooleanMap(type.prefix + "thinned", image);
	}
	void logTraced(SkeletonType type, List<SkeletonMinutia> minutiae) {
		logSkeleton(type.prefix + "traced", minutiae);
	}
	void logRemovedDots(SkeletonType type, List<SkeletonMinutia> minutiae) {
		logSkeleton(type.prefix + "removed-dots", minutiae);
	}
	void logRemovedPores(SkeletonType type, List<SkeletonMinutia> minutiae) {
		logSkeleton(type.prefix + "removed-pores", minutiae);
	}
	void logRemovedGaps(SkeletonType type, List<SkeletonMinutia> minutiae) {
		logSkeleton(type.prefix + "removed-gaps", minutiae);
	}
	void logRemovedTails(SkeletonType type, List<SkeletonMinutia> minutiae) {
		logSkeleton(type.prefix + "removed-tails", minutiae);
	}
	void logRemovedFragments(SkeletonType type, List<SkeletonMinutia> minutiae) {
		logSkeleton(type.prefix + "removed-fragments", minutiae);
	}
	void logMinutiaeSkeleton(Minutia[] minutiae) {
		logMinutiae("minutiae-skeleton", minutiae);
	}
	void logMinutiaeInner(Minutia[] minutiae) {
		logMinutiae("minutiae-inner", minutiae);
	}
	void logMinutiaeRemovedClouds(Minutia[] minutiae) {
		logMinutiae("minutiae-removed-clouds", minutiae);
	}
	void logMinutiaeClipped(Minutia[] minutiae) {
		logMinutiae("minutiae-clipped", minutiae);
	}
	void logMinutiaeShuffled(Minutia[] minutiae) {
		logMinutiae("minutiae-shuffled", minutiae);
	}
	void logEdgeTable(NeighborEdge[][] table) {
		log("edge-table", ".json", streamJson(() -> table));
	}
	void logDeserializedSize(Cell size) {
		log("deserialized-info", ".json", streamJson(() -> new DeserializedTemplateInfo(size.x, size.y)));
	}
	@AllArgsConstructor @SuppressWarnings("unused") private static class DeserializedTemplateInfo {
		int width;
		int height;
	}
	void logMinutiaeDeserialized(Minutia[] minutiae) {
		logMinutiae("minutiae-deserialized", minutiae);
	}
	void logIsoDimensions(int width, int height, int cmPixelsX, int cmPixelsY) {
		log("iso-info", ".json", streamJson(() -> new IsoTemplateInfo(width, height, cmPixelsX, cmPixelsY)));
	}
	@AllArgsConstructor @SuppressWarnings("unused") private static class IsoTemplateInfo {
		int width;
		int height;
		int xPixelsPerCM;
		int yPixelsPerCM;
	}
	void logMinutiaeIso(Minutia[] minutiae) {
		logMinutiae("minutiae-iso", minutiae);
	}
	void logEdgeHash(TIntObjectHashMap<List<IndexedEdge>> edgeHash) {
		InputStream data = new LazyByteStream() {
			@Override @SneakyThrows ByteBuffer produce() {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				DataOutputStream formatter = new DataOutputStream(buffer);
				int[] keys = edgeHash.keys();
				Arrays.sort(keys);
				formatter.writeInt(keys.length);
				for (int key : keys) {
					formatter.writeInt(key);
					List<IndexedEdge> edges = edgeHash.get(key);
					formatter.writeInt(edges.size());
					for (IndexedEdge edge : edges) {
						formatter.writeInt(edge.reference);
						formatter.writeInt(edge.neighbor);
						formatter.writeInt(edge.length);
						formatter.writeDouble(edge.referenceAngle);
						formatter.writeDouble(edge.neighborAngle);
					}
				}
				formatter.close();
				return ByteBuffer.wrap(buffer.toByteArray());
			}
		};
		log("edge-hash", ".dat", data);
	}
	void logRoots(int count, MinutiaPair[] roots) {
		log("root-pairs", ".json", streamJson(() -> Arrays.stream(roots).limit(count).map(p -> new RootPair(p.probe, p.candidate)).collect(toList())));
	}
	@AllArgsConstructor @SuppressWarnings("unused") private static class RootPair {
		int probe;
		int candidate;
	}
	void logSupportingEdge(MinutiaPair pair) {
		supportingEdges.add(new PairingEdge(pair));
	}
	void logPairing(int count, MinutiaPair[] pairs) {
		log("pairing", ".json", streamJson(() -> {
			Pairing pairing = new Pairing();
			pairing.root = new RootPair(pairs[0].probe, pairs[0].candidate);
			pairing.tree = Arrays.stream(pairs).limit(count).skip(1).map(PairingEdge::new).collect(toList());
			pairing.support = supportingEdges;
			return pairing;
		}));
		supportingEdges.clear();
	}
	@SuppressWarnings("unused") private static class Pairing {
		RootPair root;
		List<PairingEdge> tree;
		List<PairingEdge> support;
	}
	@SuppressWarnings("unused") private static class PairingEdge {
		int probeFrom;
		int probeTo;
		int candidateFrom;
		int candidateTo;
		PairingEdge(MinutiaPair pair) {
			probeFrom = pair.probeRef;
			probeTo = pair.probe;
			candidateFrom = pair.candidateRef;
			candidateTo = pair.candidate;
		}
	}
	void logScore(double minutiae, double ratio, double supported, double edge, double type, double distance, double angle, double total, double shaped) {
		log("scoring", ".json", streamJson(() -> {
			Score score = new Score();
			score.matchedMinutiaeScore = minutiae;
			score.matchedFractionOfAllMinutiaeScore = ratio;
			score.minutiaeWithSeveralEdgesScore = supported;
			score.matchedEdgesScore = edge;
			score.correctMinutiaTypeScore = type;
			score.accurateEdgeLengthScore = distance;
			score.accurateMinutiaAngleScore = angle;
			score.totalScore = total;
			score.shapedScore = shaped;
			return score;
		}));
	}
	@SuppressWarnings("unused") private static class Score {
		double matchedMinutiaeScore;
		double matchedFractionOfAllMinutiaeScore;
		double matchedEdgesScore;
		double minutiaeWithSeveralEdgesScore;
		double correctMinutiaTypeScore;
		double accurateEdgeLengthScore;
		double accurateMinutiaAngleScore;
		double totalScore;
		double shapedScore;
	}
	void logBestPairing(int nth) {
		log("best-match", ".json", streamJson(() -> new BestMatch(nth)));
	}
	@AllArgsConstructor @SuppressWarnings("unused") private static class BestMatch {
		int offset;
	}
	private void logSkeleton(String name, List<SkeletonMinutia> minutiae) {
		Supplier<Object> json = () -> {
			Map<SkeletonMinutia, Integer> offsets = new HashMap<>();
			for (int i = 0; i < minutiae.size(); ++i)
				offsets.put(minutiae.get(i), i);
			JsonSkeleton skeleton = new JsonSkeleton();
			skeleton.minutiae = minutiae.stream().map(m -> m.position).collect(toList());
			skeleton.ridges = minutiae.stream()
				.flatMap(m -> m.ridges.stream()
					.filter(r -> r.points instanceof CircularList)
					.map(r -> {
						JsonSkeletonRidge jr = new JsonSkeletonRidge();
						jr.start = offsets.get(r.start());
						jr.end = offsets.get(r.end());
						jr.length = r.points.size();
						return jr;
					}))
				.collect(toList());
			return skeleton;
		};
		InputStream data = new LazyByteStream() {
			@Override ByteBuffer produce() {
				ByteBuffer buffer = ByteBuffer.allocate(8 * minutiae.stream().flatMap(m -> m.ridges.stream()).mapToInt(r -> r.points.size()).sum());
				for (SkeletonMinutia minutia : minutiae)
					for (SkeletonRidge ridge : minutia.ridges)
						if (ridge.points instanceof CircularList)
							for (Cell at : ridge.points) {
								buffer.putInt(at.x);
								buffer.putInt(at.y);
							}
				return buffer;
			}
		};
		log(name, ".json", streamJson(json), ".dat", data);
	}
	@SuppressWarnings("unused") private static class JsonSkeleton {
		List<Cell> minutiae;
		List<JsonSkeletonRidge> ridges;
	}
	@SuppressWarnings("unused") private static class JsonSkeletonRidge {
		int start;
		int end;
		int length;
	}
	private void logMinutiae(String name, Minutia[] minutiae) {
		Supplier<Object> source = () -> Arrays.stream(minutiae).map(JsonMinutia::new).collect(toList());
		log(name, ".json", streamJson(source));
	}
	@SuppressWarnings("unused") private static class JsonMinutia {
		int x;
		int y;
		double direction;
		String type;
		JsonMinutia(Minutia minutia) {
			x = minutia.position.x;
			y = minutia.position.y;
			direction = minutia.direction;
			type = minutia.type.json;
		}
	}
	private static InputStream streamJson(Supplier<Object> source) {
		return new LazyByteStream() {
			@Override ByteBuffer produce() {
				return ByteBuffer.wrap(new Gson().toJson(source.get()).getBytes(StandardCharsets.UTF_8));
			}
		};
	}
	private void logHistogram(String name, Histogram histogram) {
		log(name, ".dat", streamHistogram(histogram), ".json", streamJson(() -> {
			ArrayDescriptor descriptor = new ArrayDescriptor();
			descriptor.axes = new String[] { "y", "x", "bin" };
			descriptor.dimensions = new int[] { histogram.height, histogram.width, histogram.depth };
			descriptor.scalar = "int";
			descriptor.bitness = 32;
			descriptor.endianness = "big";
			descriptor.format = "signed";
			return descriptor;
		}));
	}
	private static InputStream streamHistogram(Histogram histogram) {
		return new LazyByteStream() {
			@Override ByteBuffer produce() {
				ByteBuffer buffer = ByteBuffer.allocate(4 * histogram.width * histogram.height * histogram.depth);
				for (int y = 0; y < histogram.height; ++y)
					for (int x = 0; x < histogram.width; ++x)
						for (int z = 0; z < histogram.depth; ++z)
							buffer.putInt(histogram.get(x, y, z));
				return buffer;
			}
		};
	}
	private void logPointMap(String name, PointMap map) {
		log(name, ".dat", streamPointMap(map), ".json", streamJson(() -> {
			ArrayDescriptor descriptor = new ArrayDescriptor();
			descriptor.axes = new String[] { "y", "x", "axis" };
			descriptor.dimensions = new int[] { map.height, map.width, 2 };
			descriptor.scalar = "double";
			descriptor.bitness = 64;
			descriptor.endianness = "big";
			descriptor.format = "IEEE754";
			return descriptor;
		}));
	}
	private static InputStream streamPointMap(PointMap map) {
		return new LazyByteStream() {
			@Override ByteBuffer produce() {
				ByteBuffer buffer = ByteBuffer.allocate(16 * map.size().area());
				for (Cell at : map.size()) {
					Point point = map.get(at);
					buffer.putDouble(point.x);
					buffer.putDouble(point.y);
				}
				return buffer;
			}
		};
	}
	private void logDoubleMap(String name, DoubleMap map) {
		log(name, ".dat", streamDoubleMap(map), ".json", streamJson(() -> {
			ArrayDescriptor descriptor = new ArrayDescriptor();
			descriptor.axes = new String[] { "y", "x" };
			descriptor.dimensions = new int[] { map.height, map.width };
			descriptor.scalar = "double";
			descriptor.bitness = 64;
			descriptor.endianness = "big";
			descriptor.format = "IEEE754";
			return descriptor;
		}));
	}
	private static InputStream streamDoubleMap(DoubleMap map) {
		return new LazyByteStream() {
			@Override ByteBuffer produce() {
				ByteBuffer buffer = ByteBuffer.allocate(8 * map.size().area());
				for (Cell at : map.size())
					buffer.putDouble(map.get(at));
				return buffer;
			}
		};
	}
	private void logBooleanMap(String name, BooleanMap map) {
		log(name, ".dat", streamBooleanMap(map), ".json", streamJson(() -> {
			ArrayDescriptor descriptor = new ArrayDescriptor();
			descriptor.axes = new String[] { "y", "x" };
			descriptor.dimensions = new int[] { map.height, map.width };
			descriptor.scalar = "boolean";
			descriptor.bitness = 8;
			descriptor.endianness = "NA";
			descriptor.format = "false as 0, true as 1";
			return descriptor;
		}));
	}
	private static InputStream streamBooleanMap(BooleanMap map) {
		return new LazyByteStream() {
			@Override ByteBuffer produce() {
				ByteBuffer buffer = ByteBuffer.allocate(map.size().area());
				for (Cell at : map.size())
					buffer.put((byte)(map.get(at) ? 1 : 0));
				return buffer;
			}
		};
	}
	@SuppressWarnings("unused") private static class ArrayDescriptor {
		String[] axes;
		int[] dimensions;
		String scalar;
		int bitness;
		String endianness;
		String format;
	}
	private abstract static class LazyByteStream extends InputStream {
		ByteBuffer buffer;
		abstract ByteBuffer produce();
		@Override public int read() throws IOException {
			if (buffer == null)
				buffer = produce();
			if (!buffer.hasRemaining())
				return -1;
			return buffer.get();
		}
	}
	private void log(String name, String suffix, InputStream stream) {
		Map<String, InputStream> map = new HashMap<>();
		map.put(suffix, stream);
		log(name, map);
	}
	private void log(String name, String suffix1, InputStream stream1, String suffix2, InputStream stream2) {
		Map<String, InputStream> map = new HashMap<>();
		map.put(suffix1, stream1);
		map.put(suffix2, stream2);
		log(name, map);
	}
}
