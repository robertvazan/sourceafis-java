// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;
import com.google.gson.*;
import gnu.trove.map.hash.*;

public abstract class FingerprintTransparency implements AutoCloseable {
	private static final ThreadLocal<FingerprintTransparency> current = new ThreadLocal<>();
	private FingerprintTransparency outer;
	private boolean closed;
	private List<JsonEdge> supportingEdges = new ArrayList<>();
	static final FingerprintTransparency none = new FingerprintTransparency() {
		@Override protected void log(String name, Map<String, Supplier<ByteBuffer>> data) {
		}
	};
	protected abstract void log(String name, Map<String, Supplier<ByteBuffer>> data);
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
	public static FingerprintTransparency zip(OutputStream stream) {
		return new TransparencyZip(stream);
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
		log("block-map", ".json", json(() -> new JsonBlockMap(blocks)));
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
	void logTraced(Skeleton skeleton) {
		logSkeleton("traced", skeleton);
	}
	void logRemovedDots(Skeleton skeleton) {
		logSkeleton("removed-dots", skeleton);
	}
	void logRemovedPores(Skeleton skeleton) {
		logSkeleton("removed-pores", skeleton);
	}
	void logRemovedGaps(Skeleton skeleton) {
		logSkeleton("removed-gaps", skeleton);
	}
	void logRemovedTails(Skeleton skeleton) {
		logSkeleton("removed-tails", skeleton);
	}
	void logRemovedFragments(Skeleton skeleton) {
		logSkeleton("removed-fragments", skeleton);
	}
	void logMinutiaeSkeleton(FingerprintTemplate template) {
		logMinutiae("minutiae-skeleton", template);
	}
	void logMinutiaeInner(FingerprintTemplate template) {
		logMinutiae("minutiae-inner", template);
	}
	void logMinutiaeRemovedClouds(FingerprintTemplate template) {
		logMinutiae("minutiae-removed-clouds", template);
	}
	void logMinutiaeClipped(FingerprintTemplate template) {
		logMinutiae("minutiae-clipped", template);
	}
	void logMinutiaeShuffled(FingerprintTemplate template) {
		logMinutiae("minutiae-shuffled", template);
	}
	void logEdgeTable(NeighborEdge[][] table) {
		log("edge-table", ".json", json(() -> table));
	}
	void logMinutiaeDeserialized(FingerprintTemplate template) {
		logMinutiae("minutiae-deserialized", template);
	}
	void logIsoDimensions(int width, int height, int cmPixelsX, int cmPixelsY) {
		if (logging())
			log("iso-info", ".json", json(() -> new JsonIsoInfo(width, height, cmPixelsX, cmPixelsY)));
	}
	void logMinutiaeIso(FingerprintTemplate template) {
		logMinutiae("minutiae-iso", template);
	}
	void logEdgeHash(TIntObjectHashMap<List<IndexedEdge>> edgeHash) {
		log("edge-hash", ".dat", () -> IndexedEdge.serialize(edgeHash));
	}
	void logRoots(int count, MinutiaPair[] roots) {
		if (logging())
			log("root-pairs", ".json", json(() -> JsonPair.roots(count, roots)));
	}
	void logSupportingEdge(MinutiaPair pair) {
		if (logging())
			supportingEdges.add(new JsonEdge(pair));
	}
	void logPairing(int count, MinutiaPair[] pairs) {
		if (logging()) {
			log("pairing", ".json", json(() -> new JsonPairing(count, pairs, supportingEdges)));
			supportingEdges.clear();
		}
	}
	void logScore(double minutiae, double ratio, double supported, double edge, double type, double distance, double angle, double total, double shaped) {
		if (logging()) {
			log("scoring", ".json", json(() -> {
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
	}
	void logBestPairing(int nth) {
		if (logging())
			log("best-match", ".json", json(() -> new JsonBestMatch(nth)));
	}
	private void logSkeleton(String name, Skeleton skeleton) {
		log(skeleton.type.prefix + name, ".json", json(() -> new JsonSkeleton(skeleton)), ".dat", skeleton::serialize);
	}
	private void logMinutiae(String name, FingerprintTemplate template) {
		if (logging())
			log(name, ".json", json(() -> new JsonTemplate(template.size, template.minutiae)));
	}
	private void logHistogram(String name, Histogram histogram) {
		log(name, ".dat", histogram::serialize, ".json", json(histogram::json));
	}
	private void logPointMap(String name, PointMap map) {
		log(name, ".dat", map::serialize, ".json", json(map::json));
	}
	private void logDoubleMap(String name, DoubleMap map) {
		log(name, ".dat", map::serialize, ".json", json(map::json));
	}
	private void logBooleanMap(String name, BooleanMap map) {
		log(name, ".dat", map::serialize, ".json", json(map::json));
	}
	private Supplier<ByteBuffer> json(Supplier<Object> supplier) {
		return () -> ByteBuffer.wrap(new GsonBuilder().setPrettyPrinting().create().toJson(supplier.get()).getBytes(StandardCharsets.UTF_8));
	}
	private void log(String name, String suffix, Supplier<ByteBuffer> supplier) {
		Map<String, Supplier<ByteBuffer>> map = new HashMap<>();
		map.put(suffix, supplier);
		log(name, map);
	}
	private void log(String name, String suffix1, Supplier<ByteBuffer> supplier1, String suffix2, Supplier<ByteBuffer> supplier2) {
		Map<String, Supplier<ByteBuffer>> map = new HashMap<>();
		map.put(suffix1, supplier1);
		map.put(suffix2, supplier2);
		log(name, map);
	}
}
