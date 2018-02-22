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
	void logDecodedImage(DoubleMap image) {
		logDoubleMap("decoded-image", image);
	}
	void logScaledImage(DoubleMap image) {
		logDoubleMap("scaled-image", image);
	}
	void logBlockMap(BlockMap blocks) {
		log("block-map", ".json", json(() -> new JsonBlockMap(blocks)));
	}
	void logHistogram(Histogram histogram) {
		logHistogram("histogram", histogram);
	}
	void logSmoothedHistogram(Histogram histogram) {
		logHistogram("smoothed-histogram", histogram);
	}
	void logClippedContrast(DoubleMap contrast) {
		logDoubleMap("clipped-contrast", contrast);
	}
	void logAbsoluteContrastMask(BooleanMap mask) {
		logBooleanMap("absolute-contrast-mask", mask);
	}
	void logRelativeContrastMask(BooleanMap mask) {
		logBooleanMap("relative-contrast-mask", mask);
	}
	void logCombinedMask(BooleanMap mask) {
		logBooleanMap("combined-mask", mask);
	}
	void logFilteredMask(BooleanMap mask) {
		logBooleanMap("filtered-mask", mask);
	}
	void logEqualizedImage(DoubleMap image) {
		logDoubleMap("equalized-image", image);
	}
	void logPixelwiseOrientation(PointMap orientations) {
		logPointMap("pixelwise-orientation", orientations);
	}
	void logBlockOrientation(PointMap orientations) {
		logPointMap("block-orientation", orientations);
	}
	void logSmoothedOrientation(PointMap orientations) {
		logPointMap("smoothed-orientation", orientations);
	}
	void logParallelSmoothing(DoubleMap smoothed) {
		logDoubleMap("parallel-smoothing", smoothed);
	}
	void logOrthogonalSmoothing(DoubleMap smoothed) {
		logDoubleMap("orthogonal-smoothing", smoothed);
	}
	void logBinarizedImage(BooleanMap image) {
		logBooleanMap("binarized-image", image);
	}
	void logFilteredBinarydImage(BooleanMap image) {
		logBooleanMap("filtered-binary-image", image);
	}
	void logPixelMask(BooleanMap image) {
		logBooleanMap("pixel-mask", image);
	}
	void logInnerMask(BooleanMap image) {
		logBooleanMap("inner-mask", image);
	}
	void logBinarizedSkeleton(SkeletonType type, BooleanMap image) {
		logBooleanMap(type.prefix + "binarized-skeleton", image);
	}
	void logThinnedSkeleton(SkeletonType type, BooleanMap image) {
		logBooleanMap(type.prefix + "thinned-skeleton", image);
	}
	void logTracedSkeleton(Skeleton skeleton) {
		logSkeleton("traced-skeleton", skeleton);
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
	void logSkeletonMinutiae(FingerprintTemplate template) {
		logMinutiae("skeleton-minutiae", template);
	}
	void logInnerMinutiae(FingerprintTemplate template) {
		logMinutiae("inner-minutiae", template);
	}
	void logRemovedMinutiaClouds(FingerprintTemplate template) {
		logMinutiae("removed-minutia-clouds", template);
	}
	void logTopMinutiae(FingerprintTemplate template) {
		logMinutiae("top-minutiae", template);
	}
	void logShuffledMinutiae(FingerprintTemplate template) {
		logMinutiae("shuffled-minutiae", template);
	}
	void logEdgeTable(NeighborEdge[][] table) {
		log("edge-table", ".json", json(() -> table));
	}
	void logDeserializedMinutiae(FingerprintTemplate template) {
		logMinutiae("deserialized-minutiae", template);
	}
	void logIsoMetadata(int width, int height, int cmPixelsX, int cmPixelsY) {
		if (logging())
			log("iso-metadata", ".json", json(() -> new JsonIsoMetadata(width, height, cmPixelsX, cmPixelsY)));
	}
	void logIsoMinutiae(FingerprintTemplate template) {
		logMinutiae("iso-minutiae", template);
	}
	void logEdgeHash(TIntObjectHashMap<List<IndexedEdge>> edgeHash) {
		log("edge-hash", ".dat", () -> IndexedEdge.serialize(edgeHash));
	}
	void logRootPairs(int count, MinutiaPair[] roots) {
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
			log("score", ".json", json(() -> {
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
	void logBestMatch(int nth) {
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
