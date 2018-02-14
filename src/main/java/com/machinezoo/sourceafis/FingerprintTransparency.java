// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import gnu.trove.map.hash.*;

public abstract class FingerprintTransparency implements AutoCloseable {
	private static final ThreadLocal<FingerprintTransparency> current = new ThreadLocal<>();
	private FingerprintTransparency outer;
	private boolean closed;
	private List<JsonEdge> supportingEdges = new ArrayList<>();
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
		return new TransparencyZip(zip);
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
		log("block-map", ".json", LazyByteStream.json(() -> new JsonBlockMap(blocks)));
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
		log("edge-table", ".json", LazyByteStream.json(() -> table));
	}
	void logDeserializedSize(Cell size) {
		log("deserialized-info", ".json", LazyByteStream.json(() -> new JsonDeserializedInfo(size.x, size.y)));
	}
	void logMinutiaeDeserialized(Minutia[] minutiae) {
		logMinutiae("minutiae-deserialized", minutiae);
	}
	void logIsoDimensions(int width, int height, int cmPixelsX, int cmPixelsY) {
		log("iso-info", ".json", LazyByteStream.json(() -> new JsonIsoInfo(width, height, cmPixelsX, cmPixelsY)));
	}
	void logMinutiaeIso(Minutia[] minutiae) {
		logMinutiae("minutiae-iso", minutiae);
	}
	void logEdgeHash(TIntObjectHashMap<List<IndexedEdge>> edgeHash) {
		log("edge-hash", ".dat", IndexedEdge.stream(edgeHash));
	}
	void logRoots(int count, MinutiaPair[] roots) {
		log("root-pairs", ".json", LazyByteStream.json(() -> JsonPair.roots(count, roots)));
	}
	void logSupportingEdge(MinutiaPair pair) {
		if (logging())
			supportingEdges.add(new JsonEdge(pair));
	}
	void logPairing(int count, MinutiaPair[] pairs) {
		log("pairing", ".json", LazyByteStream.json(() -> new JsonPairing(count, pairs, supportingEdges)));
		supportingEdges.clear();
	}
	void logScore(double minutiae, double ratio, double supported, double edge, double type, double distance, double angle, double total, double shaped) {
		log("scoring", ".json", LazyByteStream.json(() -> {
			JsonScore score = new JsonScore();
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
	void logBestPairing(int nth) {
		log("best-match", ".json", LazyByteStream.json(() -> new JsonBestMatch(nth)));
	}
	private void logSkeleton(String name, List<SkeletonMinutia> minutiae) {
		log(name, ".json", LazyByteStream.json(() -> new JsonSkeleton(minutiae)), ".dat", new LazyByteStream(() -> SkeletonMinutia.serialize(minutiae)));
	}
	private void logMinutiae(String name, Minutia[] minutiae) {
		log(name, ".json", LazyByteStream.json(() -> JsonMinutia.map(minutiae)));
	}
	private void logHistogram(String name, Histogram histogram) {
		log(name, ".dat", new LazyByteStream(histogram::serialize), ".json", LazyByteStream.json(histogram::json));
	}
	private void logPointMap(String name, PointMap map) {
		log(name, ".dat", new LazyByteStream(map::serialize), ".json", LazyByteStream.json(map::json));
	}
	private void logDoubleMap(String name, DoubleMap map) {
		log(name, ".dat", new LazyByteStream(map::serialize), ".json", LazyByteStream.json(map::json));
	}
	private void logBooleanMap(String name, BooleanMap map) {
		log(name, ".dat", new LazyByteStream(map::serialize), ".json", LazyByteStream.json(map::json));
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
