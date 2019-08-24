// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;
import com.google.gson.*;
import gnu.trove.map.hash.*;

/**
 * Algorithm transparency API that can capture all intermediate data structures produced by SourceAFIS algorithm.
 * See <a href="https://sourceafis.machinezoo.com/transparency/">algorithm transparency</a> pages
 * on SourceAFIS website for more information and a tutorial on how to use this class.
 * <p>
 * Applications can subclass {@code FingerprintTransparency} and override
 * {@link #log(String, Map)} method to define new transparency data logger.
 * One default implementation of {@code FingerprintTransparency} is returned by {@link #zip(OutputStream)} method.
 * <p>
 * An instance of {@code FingerprintTransparency} must be passed to
 * {@link FingerprintTemplate#transparency(FingerprintTransparency)} or {@link FingerprintMatcher#transparency(FingerprintTransparency)}
 * for transparency data to be actually collected.
 * <p>
 * This class implements {@link AutoCloseable} and callers must ensure that {@link #close()} is called,
 * perhaps by using try-with-resources construct, unless the particular subclass is known to not need any cleanup.
 *
 * @see <a href="https://sourceafis.machinezoo.com/transparency/">Algorithm transparency in SourceAFIS</a>
 * @see FingerprintTemplate#transparency(FingerprintTransparency)
 * @see FingerprintMatcher#transparency(FingerprintTransparency)
 */
public abstract class FingerprintTransparency implements AutoCloseable {
	private List<JsonEdge> supportingEdges = new ArrayList<>();
	static final FingerprintTransparency none = new FingerprintTransparency() {
		@Override protected void log(String name, Map<String, Supplier<ByteBuffer>> data) {
		}
	};
	/**
	 * Creates an instance of {@code FingerprintTransparency}.
	 * {@code FingerprintTransparency} is an abstract class.
	 * This empty constructor is only called by subclasses.
	 */
	protected FingerprintTransparency() {
	}
	/**
	 * Record transparency data. This is an abstract method that subclasses must override.
	 * If algorithm transparency is enabled by passing an instance of {@code FingerprintTransparency}
	 * to {@link FingerprintTemplate#transparency(FingerprintTransparency)} or {@link FingerprintMatcher#transparency(FingerprintTransparency)},
	 * this method is called with transparency data in its parameters.
	 * <p>
	 * Parameter {@code keyword} specifies the kind of transparency data being logged,
	 * usually corresponding to some stage in the algorithm.
	 * For convenience, several related pieces of transparency data are reported together.
	 * All pieces are available via map in parameter {@code data},
	 * keyed by file suffix identifying the kind of data,
	 * usually {@code .json} or {@code .dat} for JSON and binary data respectively.
	 * See <a href="https://sourceafis.machinezoo.com/transparency/">algorithm transparency</a>
	 * on SourceAFIS website for documentation of the structure of the transparency data.
	 * <p>
	 * Transparency data is offered indirectly via {@link Supplier}.
	 * If this {@code Supplier} is not evaluated, the data is never serialized.
	 * This allows applications to efficiently collect only transparency data that is actually needed.
	 * 
	 * @param keyword
	 *            specifies the kind of transparency data being reported
	 * @param data
	 *            a map of suffixes (like {@code .json} or {@code .dat}) to {@link Supplier} of the available transparency data
	 * 
	 * @see <a href="https://sourceafis.machinezoo.com/transparency/">Algorithm transparency in SourceAFIS</a>
	 * @see #zip(OutputStream)
	 */
	protected abstract void log(String keyword, Map<String, Supplier<ByteBuffer>> data);
	/**
	 * Release system resources held by this instance if any.
	 * Subclasses can override this method to perform cleanup.
	 * Default implementation of this method is empty.
	 */
	@Override public void close() {
	}
	/**
	 * Write all transparency data to a ZIP file.
	 * This is a convenience method to enable easy exploration of the available data.
	 * Programmatic processing of transparency data should be done by subclassing {@code FingerprintTransparency}
	 * and overriding {@link #log(String, Map)} method.
	 * <p>
	 * The returned {@code FingerprintTransparency} object holds system resources
	 * and callers are responsible for calling {@link #close()} method, perhaps using try-with-resources construct.
	 * Failure to close the returned {@code FingerprintTransparency} instance may result in damaged ZIP file.
	 * 
	 * @param stream
	 *            output stream where ZIP file will be written (will be closed when the returned {@code FingerprintTransparency} is closed)
	 * @return algorithm transparency logger that writes data to a ZIP file
	 * 
	 * @see #close()
	 * @see #log(String, Map)
	 */
	public static FingerprintTransparency zip(OutputStream stream) {
		return new TransparencyZip(stream);
	}
	boolean logging() {
		return this != none;
	}
	// https://sourceafis.machinezoo.com/transparency/decoded-image
	void logDecodedImage(DoubleMap image) {
		logDoubleMap("decoded-image", image);
	}
	// https://sourceafis.machinezoo.com/transparency/scaled-image
	void logScaledImage(DoubleMap image) {
		logDoubleMap("scaled-image", image);
	}
	// https://sourceafis.machinezoo.com/transparency/block-map
	void logBlockMap(BlockMap blocks) {
		log("block-map", ".json", json(() -> blocks));
	}
	// https://sourceafis.machinezoo.com/transparency/histogram
	void logHistogram(HistogramMap histogram) {
		logHistogram("histogram", histogram);
	}
	// https://sourceafis.machinezoo.com/transparency/smoothed-histogram
	void logSmoothedHistogram(HistogramMap histogram) {
		logHistogram("smoothed-histogram", histogram);
	}
	// https://sourceafis.machinezoo.com/transparency/clipped-contrast
	void logClippedContrast(DoubleMap contrast) {
		logDoubleMap("clipped-contrast", contrast);
	}
	// https://sourceafis.machinezoo.com/transparency/absolute-contrast-mask
	void logAbsoluteContrastMask(BooleanMap mask) {
		logBooleanMap("absolute-contrast-mask", mask);
	}
	// https://sourceafis.machinezoo.com/transparency/relative-contrast-mask
	void logRelativeContrastMask(BooleanMap mask) {
		logBooleanMap("relative-contrast-mask", mask);
	}
	// https://sourceafis.machinezoo.com/transparency/combined-mask
	void logCombinedMask(BooleanMap mask) {
		logBooleanMap("combined-mask", mask);
	}
	// https://sourceafis.machinezoo.com/transparency/filtered-mask
	void logFilteredMask(BooleanMap mask) {
		logBooleanMap("filtered-mask", mask);
	}
	// https://sourceafis.machinezoo.com/transparency/equalized-image
	void logEqualizedImage(DoubleMap image) {
		logDoubleMap("equalized-image", image);
	}
	// https://sourceafis.machinezoo.com/transparency/pixelwise-orientation
	void logPixelwiseOrientation(DoublePointMap orientations) {
		logPointMap("pixelwise-orientation", orientations);
	}
	// https://sourceafis.machinezoo.com/transparency/block-orientation
	void logBlockOrientation(DoublePointMap orientations) {
		logPointMap("block-orientation", orientations);
	}
	// https://sourceafis.machinezoo.com/transparency/smoothed-orientation
	void logSmoothedOrientation(DoublePointMap orientations) {
		logPointMap("smoothed-orientation", orientations);
	}
	// https://sourceafis.machinezoo.com/transparency/parallel-smoothing
	void logParallelSmoothing(DoubleMap smoothed) {
		logDoubleMap("parallel-smoothing", smoothed);
	}
	// https://sourceafis.machinezoo.com/transparency/orthogonal-smoothing
	void logOrthogonalSmoothing(DoubleMap smoothed) {
		logDoubleMap("orthogonal-smoothing", smoothed);
	}
	// https://sourceafis.machinezoo.com/transparency/binarized-image
	void logBinarizedImage(BooleanMap image) {
		logBooleanMap("binarized-image", image);
	}
	// https://sourceafis.machinezoo.com/transparency/filtered-binary-image
	void logFilteredBinarydImage(BooleanMap image) {
		logBooleanMap("filtered-binary-image", image);
	}
	// https://sourceafis.machinezoo.com/transparency/pixel-mask
	void logPixelMask(BooleanMap image) {
		logBooleanMap("pixel-mask", image);
	}
	// https://sourceafis.machinezoo.com/transparency/inner-mask
	void logInnerMask(BooleanMap image) {
		logBooleanMap("inner-mask", image);
	}
	// https://sourceafis.machinezoo.com/transparency/binarized-skeleton
	void logBinarizedSkeleton(SkeletonType type, BooleanMap image) {
		logBooleanMap(type.prefix + "binarized-skeleton", image);
	}
	// https://sourceafis.machinezoo.com/transparency/thinned-skeleton
	void logThinnedSkeleton(SkeletonType type, BooleanMap image) {
		logBooleanMap(type.prefix + "thinned-skeleton", image);
	}
	// https://sourceafis.machinezoo.com/transparency/traced-skeleton
	void logTracedSkeleton(Skeleton skeleton) {
		logSkeleton("traced-skeleton", skeleton);
	}
	// https://sourceafis.machinezoo.com/transparency/removed-dots
	void logRemovedDots(Skeleton skeleton) {
		logSkeleton("removed-dots", skeleton);
	}
	// https://sourceafis.machinezoo.com/transparency/removed-pores
	void logRemovedPores(Skeleton skeleton) {
		logSkeleton("removed-pores", skeleton);
	}
	// https://sourceafis.machinezoo.com/transparency/removed-gaps
	void logRemovedGaps(Skeleton skeleton) {
		logSkeleton("removed-gaps", skeleton);
	}
	// https://sourceafis.machinezoo.com/transparency/removed-tails
	void logRemovedTails(Skeleton skeleton) {
		logSkeleton("removed-tails", skeleton);
	}
	// https://sourceafis.machinezoo.com/transparency/removed-fragments
	void logRemovedFragments(Skeleton skeleton) {
		logSkeleton("removed-fragments", skeleton);
	}
	// https://sourceafis.machinezoo.com/transparency/skeleton-minutiae
	void logSkeletonMinutiae(TemplateBuilder template) {
		logMinutiae("skeleton-minutiae", template);
	}
	// https://sourceafis.machinezoo.com/transparency/inner-minutiae
	void logInnerMinutiae(TemplateBuilder template) {
		logMinutiae("inner-minutiae", template);
	}
	// https://sourceafis.machinezoo.com/transparency/removed-minutia-clouds
	void logRemovedMinutiaClouds(TemplateBuilder template) {
		logMinutiae("removed-minutia-clouds", template);
	}
	// https://sourceafis.machinezoo.com/transparency/top-minutiae
	void logTopMinutiae(TemplateBuilder template) {
		logMinutiae("top-minutiae", template);
	}
	// https://sourceafis.machinezoo.com/transparency/shuffled-minutiae
	void logShuffledMinutiae(TemplateBuilder template) {
		logMinutiae("shuffled-minutiae", template);
	}
	// https://sourceafis.machinezoo.com/transparency/edge-table
	void logEdgeTable(NeighborEdge[][] table) {
		log("edge-table", ".json", json(() -> table));
	}
	// https://sourceafis.machinezoo.com/transparency/deserialized-minutiae
	void logDeserializedMinutiae(TemplateBuilder template) {
		logMinutiae("deserialized-minutiae", template);
	}
	// https://sourceafis.machinezoo.com/transparency/edge-hash
	void logEdgeHash(TIntObjectHashMap<List<IndexedEdge>> edgeHash) {
		log("edge-hash", ".dat", () -> IndexedEdge.serialize(edgeHash));
	}
	// https://sourceafis.machinezoo.com/transparency/root-pairs
	void logRootPairs(int count, MinutiaPair[] roots) {
		if (logging())
			log("root-pairs", ".json", json(() -> JsonPair.roots(count, roots)));
	}
	// Accumulated and then added to https://sourceafis.machinezoo.com/transparency/pairing
	void logSupportingEdge(MinutiaPair pair) {
		if (logging())
			supportingEdges.add(new JsonEdge(pair));
	}
	// https://sourceafis.machinezoo.com/transparency/pairing
	void logPairing(int count, MinutiaPair[] pairs) {
		if (logging()) {
			log("pairing", ".json", json(() -> new JsonPairing(count, pairs, supportingEdges)));
			supportingEdges.clear();
		}
	}
	// https://sourceafis.machinezoo.com/transparency/score
	void logScore(Score score) {
		if (logging())
			log("score", ".json", json(() -> score));
	}
	// https://sourceafis.machinezoo.com/transparency/best-match
	void logBestMatch(int nth) {
		if (logging())
			log("best-match", ".json", json(() -> new JsonBestMatch(nth)));
	}
	private void logSkeleton(String name, Skeleton skeleton) {
		log(skeleton.type.prefix + name, ".json", json(() -> new JsonSkeleton(skeleton)), ".dat", skeleton::serialize);
	}
	private void logMinutiae(String name, TemplateBuilder template) {
		if (logging())
			log(name, ".json", json(() -> new JsonTemplate(template.size, template.minutiae)));
	}
	private void logHistogram(String name, HistogramMap histogram) {
		log(name, ".dat", histogram::serialize, ".json", json(histogram::json));
	}
	private void logPointMap(String name, DoublePointMap map) {
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
