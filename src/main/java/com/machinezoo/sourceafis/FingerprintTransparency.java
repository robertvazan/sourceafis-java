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
	void logDecodedImage(DoubleMap image) {
		logDoubleMap("decoded-image", image);
	}
	void logScaledImage(DoubleMap image) {
		logDoubleMap("scaled-image", image);
	}
	void logBlockMap(BlockMap blocks) {
		log("block-map", ".json", json(() -> blocks));
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
	void logSkeletonMinutiae(TemplateBuilder template) {
		logMinutiae("skeleton-minutiae", template);
	}
	void logInnerMinutiae(TemplateBuilder template) {
		logMinutiae("inner-minutiae", template);
	}
	void logRemovedMinutiaClouds(TemplateBuilder template) {
		logMinutiae("removed-minutia-clouds", template);
	}
	void logTopMinutiae(TemplateBuilder template) {
		logMinutiae("top-minutiae", template);
	}
	void logShuffledMinutiae(TemplateBuilder template) {
		logMinutiae("shuffled-minutiae", template);
	}
	void logEdgeTable(NeighborEdge[][] table) {
		log("edge-table", ".json", json(() -> table));
	}
	void logDeserializedMinutiae(TemplateBuilder template) {
		logMinutiae("deserialized-minutiae", template);
	}
	void logIsoMetadata(int width, int height, int cmPixelsX, int cmPixelsY) {
		if (logging())
			log("iso-metadata", ".json", json(() -> new JsonIsoMetadata(width, height, cmPixelsX, cmPixelsY)));
	}
	void logIsoMinutiae(TemplateBuilder template) {
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
	void logScore(Score score) {
		if (logging())
			log("score", ".json", json(() -> score));
	}
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
