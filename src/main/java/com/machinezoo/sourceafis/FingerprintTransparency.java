// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.zip.*;
import com.google.gson.*;
import com.machinezoo.noexception.*;
import gnu.trove.map.hash.*;

/**
 * Algorithm transparency API that can capture all intermediate data structures produced by SourceAFIS algorithm.
 * See <a href="https://sourceafis.machinezoo.com/transparency/">algorithm transparency</a> pages
 * on SourceAFIS website for more information and a tutorial on how to use this class.
 * <p>
 * Applications can subclass {@code FingerprintTransparency} and override
 * {@link #capture(String, Map)} method to define new transparency data logger.
 * One default implementation of {@code FingerprintTransparency} is returned by {@link #zip(OutputStream)} method.
 * <p>
 * {@code FingerprintTransparency} instance should be created in try-with-resource construct.
 * It will be capturing transparency data from all operations on current thread
 * between invocation of the constructor and invocation of {@link #close()} method,
 * which happens automatically in try-with-resource construct.
 *
 * @see <a href="https://sourceafis.machinezoo.com/transparency/">Algorithm transparency in SourceAFIS</a>
 */
public abstract class FingerprintTransparency implements AutoCloseable {
	/*
	 * Having transparency objects tied to current thread spares us of contaminating all classes with transparency APIs.
	 * Transparency object is activated on the thread the moment it is created.
	 * Having no explicit activation makes for a bit simpler API.
	 */
	private static final ThreadLocal<FingerprintTransparency> current = new ThreadLocal<>();
	private FingerprintTransparency outer;
	/**
	 * Creates an instance of {@code FingerprintTransparency} and activates it.
	 * <p>
	 * Activation places the new {@code FingerprintTransparency} instance in thread-local storage,
	 * which causes all operations executed by current thread to log data to this {@code FingerprintTransparency} instance.
	 * If activations are nested, data is only logged to the currently innermost {@code FingerprintTransparency}.
	 * <p>
	 * Deactivation happens in {@link #close()} method.
	 * Instances of {@code FingerprintTransparency} should be created in try-with-resources construct
	 * to ensure that {@link #close()} is always called.
	 * <p>
	 * {@code FingerprintTransparency} is an abstract class.
	 * This constructor is only called by subclasses.
	 * 
	 * @see #close()
	 */
	protected FingerprintTransparency() {
		outer = current.get();
		current.set(this);
	}
	/**
	 * Record transparency data. Subclasses must override this method, because the default implementation does nothing.
	 * While this {@code FingerprintTransparency} object is active (between call to the constructor and call to {@link #close()}),
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
	 * <p>
	 * If this method throws, exception is propagated through SourceAFIS code.
	 * 
	 * @param keyword
	 *            specifies the kind of transparency data being reported
	 * @param data
	 *            a map of suffixes (like {@code .json} or {@code .dat}) to {@link Supplier} of the available transparency data
	 * 
	 * @see <a href="https://sourceafis.machinezoo.com/transparency/">Algorithm transparency in SourceAFIS</a>
	 * @see #zip(OutputStream)
	 */
	protected void capture(String keyword, Map<String, Supplier<byte[]>> data) {
		/*
		 * If nobody overrode this method, assume it is legacy code and call the old log() method.
		 */
		Map<String, Supplier<ByteBuffer>> translated = new HashMap<>();
		for (Map.Entry<String, Supplier<byte[]>> entry : data.entrySet())
			translated.put(entry.getKey(), () -> ByteBuffer.wrap(entry.getValue().get()));
		log(keyword, translated);
	}
	/**
	 * Record transparency data in buffers.
	 * This is a deprecated variant of {@link #capture(String, Map)}
	 * that uses {@link ByteBuffer} instead of plain byte arrays.
	 * This method is only called if {@link #capture(String, Map)} is not overridden.
	 * 
	 * @param keyword
	 *            specifies the kind of transparency data being reported
	 * @param data
	 *            a map of suffixes (like {@code .json} or {@code .dat}) to {@link Supplier} of the available transparency data
	 * 
	 * @see <a href="https://sourceafis.machinezoo.com/transparency/">Algorithm transparency in SourceAFIS</a>
	 * @see #capture(String, Map)
	 */
	@Deprecated protected void log(String keyword, Map<String, Supplier<ByteBuffer>> data) {
	}
	/**
	 * Deactivate transparency logging and release system resources held by this instance if any.
	 * This method is normally called automatically when {@code FingerprintTransparency} is used in try-with-resources construct.
	 * <p>
	 * Deactivation stops transparency data logging to this instance of {@code FingerprintTransparency},
	 * which was started by the constructor ({@link #FingerprintTransparency()}).
	 * If activations were nested, this method reactivates the outer {@code FingerprintTransparency}.
	 * <p>
	 * Subclasses can override this method to perform cleanup.
	 * Default implementation of this method performs deactivation.
	 * It must be called by overriding methods for deactivation to work correctly.
	 * <p>
	 * This method doesn't declare any checked exceptions in order to spare callers of mandatory exception checking.
	 * If your code needs to throw a checked exception, wrap it in an unchecked exception.
	 * 
	 * @see #FingerprintTransparency()
	 */
	@Override public void close() {
		current.set(outer);
		outer = null;
	}
	/**
	 * Write all transparency data to a ZIP file.
	 * This is a convenience method to enable easy exploration of the available data.
	 * Programmatic processing of transparency data should be done by subclassing {@code FingerprintTransparency}
	 * and overriding {@link #capture(String, Map)} method.
	 * <p>
	 * The returned {@code FingerprintTransparency} object holds system resources
	 * and callers are responsible for calling {@link #close()} method, perhaps using try-with-resources construct.
	 * Failure to close the returned {@code FingerprintTransparency} instance may result in damaged ZIP file.
	 * <p>
	 * If the provided {@code stream} throws {@link IOException},
	 * the exception will be wrapped in an unchecked exception and propagated.
	 * 
	 * @param stream
	 *            output stream where ZIP file will be written (will be closed when the returned {@code FingerprintTransparency} is closed)
	 * @return algorithm transparency logger that writes data to a ZIP file
	 * 
	 * @see #close()
	 * @see #capture(String, Map)
	 */
	public static FingerprintTransparency zip(OutputStream stream) {
		return new TransparencyZip(stream);
	}
	private static class TransparencyZip extends FingerprintTransparency {
		private final ZipOutputStream zip;
		private int offset;
		TransparencyZip(OutputStream stream) {
			zip = new ZipOutputStream(stream);
		}
		@Override protected void capture(String keyword, Map<String, Supplier<byte[]>> data) {
			Exceptions.wrap().run(() -> {
				List<String> suffixes = data.keySet().stream()
					.sorted(Comparator.comparing(ext -> {
						if (ext.equals(".json"))
							return 1;
						if (ext.equals(".dat"))
							return 2;
						return 3;
					}))
					.collect(toList());
				for (String suffix : suffixes) {
					++offset;
					zip.putNextEntry(new ZipEntry(String.format("%03d", offset) + "-" + keyword + suffix));
					zip.write(data.get(suffix).get());
					zip.closeEntry();
				}
			});
		}
		@Override public void close() {
			super.close();
			Exceptions.sneak().run(zip::close);
		}
	}
	/*
	 * To avoid null checks everywhere, we have one noop class as a fallback.
	 */
	private static final FingerprintTransparency none;
	private static class NoFingerprintTransparency extends FingerprintTransparency {
		@Override protected void capture(String keyword, Map<String, Supplier<byte[]>> data) {
		}
	}
	static {
		none = new NoFingerprintTransparency();
		/*
		 * Deactivate logging to the noop logger as soon as it is created.
		 */
		none.close();
	}
	static FingerprintTransparency current() {
		return Optional.ofNullable(current.get()).orElse(none);
	}
	/*
	 * Just preparing the data for logging may be expensive.
	 * Such code should use this method to check for active transparency data logging
	 * before any expensive logging-related operations are performed.
	 * Right now all such checks are done inside this class.
	 */
	boolean logging() {
		return this != none;
	}
	// https://sourceafis.machinezoo.com/transparency/decoded-image
	void logDecodedImage(DoubleMatrix image) {
		logDoubleMap("decoded-image", image);
	}
	// https://sourceafis.machinezoo.com/transparency/scaled-image
	void logScaledImage(DoubleMatrix image) {
		logDoubleMap("scaled-image", image);
	}
	// https://sourceafis.machinezoo.com/transparency/block-map
	void logBlockMap(BlockMap blocks) {
		log("block-map", ".json", json(() -> blocks));
	}
	// https://sourceafis.machinezoo.com/transparency/histogram
	void logHistogram(HistogramCube histogram) {
		logHistogram("histogram", histogram);
	}
	// https://sourceafis.machinezoo.com/transparency/smoothed-histogram
	void logSmoothedHistogram(HistogramCube histogram) {
		logHistogram("smoothed-histogram", histogram);
	}
	// https://sourceafis.machinezoo.com/transparency/clipped-contrast
	void logClippedContrast(DoubleMatrix contrast) {
		logDoubleMap("clipped-contrast", contrast);
	}
	// https://sourceafis.machinezoo.com/transparency/absolute-contrast-mask
	void logAbsoluteContrastMask(BooleanMatrix mask) {
		logBooleanMap("absolute-contrast-mask", mask);
	}
	// https://sourceafis.machinezoo.com/transparency/relative-contrast-mask
	void logRelativeContrastMask(BooleanMatrix mask) {
		logBooleanMap("relative-contrast-mask", mask);
	}
	// https://sourceafis.machinezoo.com/transparency/combined-mask
	void logCombinedMask(BooleanMatrix mask) {
		logBooleanMap("combined-mask", mask);
	}
	// https://sourceafis.machinezoo.com/transparency/filtered-mask
	void logFilteredMask(BooleanMatrix mask) {
		logBooleanMap("filtered-mask", mask);
	}
	// https://sourceafis.machinezoo.com/transparency/equalized-image
	void logEqualizedImage(DoubleMatrix image) {
		logDoubleMap("equalized-image", image);
	}
	// https://sourceafis.machinezoo.com/transparency/pixelwise-orientation
	void logPixelwiseOrientation(DoublePointMatrix orientations) {
		logPointMap("pixelwise-orientation", orientations);
	}
	// https://sourceafis.machinezoo.com/transparency/block-orientation
	void logBlockOrientation(DoublePointMatrix orientations) {
		logPointMap("block-orientation", orientations);
	}
	// https://sourceafis.machinezoo.com/transparency/smoothed-orientation
	void logSmoothedOrientation(DoublePointMatrix orientations) {
		logPointMap("smoothed-orientation", orientations);
	}
	// https://sourceafis.machinezoo.com/transparency/parallel-smoothing
	void logParallelSmoothing(DoubleMatrix smoothed) {
		logDoubleMap("parallel-smoothing", smoothed);
	}
	// https://sourceafis.machinezoo.com/transparency/orthogonal-smoothing
	void logOrthogonalSmoothing(DoubleMatrix smoothed) {
		logDoubleMap("orthogonal-smoothing", smoothed);
	}
	// https://sourceafis.machinezoo.com/transparency/binarized-image
	void logBinarizedImage(BooleanMatrix image) {
		logBooleanMap("binarized-image", image);
	}
	// https://sourceafis.machinezoo.com/transparency/filtered-binary-image
	void logFilteredBinarydImage(BooleanMatrix image) {
		logBooleanMap("filtered-binary-image", image);
	}
	// https://sourceafis.machinezoo.com/transparency/pixel-mask
	void logPixelMask(BooleanMatrix image) {
		logBooleanMap("pixel-mask", image);
	}
	// https://sourceafis.machinezoo.com/transparency/inner-mask
	void logInnerMask(BooleanMatrix image) {
		logBooleanMap("inner-mask", image);
	}
	// https://sourceafis.machinezoo.com/transparency/binarized-skeleton
	void logBinarizedSkeleton(SkeletonType type, BooleanMatrix image) {
		logBooleanMap(type.prefix + "binarized-skeleton", image);
	}
	// https://sourceafis.machinezoo.com/transparency/thinned-skeleton
	void logThinnedSkeleton(SkeletonType type, BooleanMatrix image) {
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
	// https://sourceafis.machinezoo.com/transparency/edge-hash
	void logEdgeHash(TIntObjectHashMap<List<IndexedEdge>> edgeHash) {
		log("edge-hash", ".dat", () -> IndexedEdge.serialize(edgeHash));
	}
	// https://sourceafis.machinezoo.com/transparency/root-pairs
	void logRootPairs(int count, MinutiaPair[] roots) {
		if (logging())
			log("root-pairs", ".json", json(() -> JsonPair.roots(count, roots)));
	}
	private List<JsonEdge> supportingEdges = new ArrayList<>();
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
	@SuppressWarnings("unused") private static class JsonPairing {
		JsonPair root;
		List<JsonEdge> tree;
		List<JsonEdge> support;
		JsonPairing(int count, MinutiaPair[] pairs, List<JsonEdge> supporting) {
			root = new JsonPair(pairs[0].probe, pairs[0].candidate);
			tree = Arrays.stream(pairs).limit(count).skip(1).map(JsonEdge::new).collect(toList());
			support = supporting;
		}
	}
	@SuppressWarnings("unused") private static class JsonPair {
		int probe;
		int candidate;
		JsonPair(int probe, int candidate) {
			this.probe = probe;
			this.candidate = candidate;
		}
		static List<JsonPair> roots(int count, MinutiaPair[] roots) {
			return Arrays.stream(roots).limit(count).map(p -> new JsonPair(p.probe, p.candidate)).collect(toList());
		}
	}
	@SuppressWarnings("unused") private static class JsonEdge {
		int probeFrom;
		int probeTo;
		int candidateFrom;
		int candidateTo;
		JsonEdge(MinutiaPair pair) {
			probeFrom = pair.probeRef;
			probeTo = pair.probe;
			candidateFrom = pair.candidateRef;
			candidateTo = pair.candidate;
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
	@SuppressWarnings("unused") private static class JsonBestMatch {
		int offset;
		JsonBestMatch(int offset) {
			this.offset = offset;
		}
	}
	private void logSkeleton(String name, Skeleton skeleton) {
		log(skeleton.type.prefix + name, ".json", json(() -> new JsonSkeleton(skeleton)), ".dat", skeleton::serialize);
	}
	@SuppressWarnings("unused") private static class JsonSkeleton {
		int width;
		int height;
		List<IntPoint> minutiae;
		List<JsonSkeletonRidge> ridges;
		JsonSkeleton(Skeleton skeleton) {
			width = skeleton.size.x;
			height = skeleton.size.y;
			Map<SkeletonMinutia, Integer> offsets = new HashMap<>();
			for (int i = 0; i < skeleton.minutiae.size(); ++i)
				offsets.put(skeleton.minutiae.get(i), i);
			this.minutiae = skeleton.minutiae.stream().map(m -> m.position).collect(toList());
			ridges = skeleton.minutiae.stream()
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
		}
	}
	@SuppressWarnings("unused") private static class JsonSkeletonRidge {
		int start;
		int end;
		int length;
	}
	private void logMinutiae(String name, TemplateBuilder template) {
		if (logging())
			log(name, ".json", json(() -> new JsonTemplate(template.size, template.minutiae)));
	}
	private void logHistogram(String name, HistogramCube histogram) {
		log(name, ".dat", histogram::serialize, ".json", json(histogram::json));
	}
	private void logPointMap(String name, DoublePointMatrix matrix) {
		log(name, ".dat", matrix::serialize, ".json", json(matrix::json));
	}
	private void logDoubleMap(String name, DoubleMatrix matrix) {
		log(name, ".dat", matrix::serialize, ".json", json(matrix::json));
	}
	private void logBooleanMap(String name, BooleanMatrix matrix) {
		log(name, ".dat", matrix::serialize, ".json", json(matrix::json));
	}
	private Supplier<byte[]> json(Supplier<Object> supplier) {
		return () -> new GsonBuilder().setPrettyPrinting().create().toJson(supplier.get()).getBytes(StandardCharsets.UTF_8);
	}
	private AtomicInteger loggedVersion = new AtomicInteger();
	private void logVersion() {
		if (logging() && loggedVersion.getAndSet(1) == 0) {
			Map<String, Supplier<byte[]>> map = new HashMap<>();
			map.put(".json", json(() -> new JsonVersion(FingerprintCompatibility.version())));
			capture("version", map);
		}
	}
	@SuppressWarnings("unused") private static class JsonVersion {
		String version;
		JsonVersion(String version) {
			this.version = version;
		}
	}
	private void log(String name, String suffix, Supplier<byte[]> supplier) {
		Map<String, Supplier<byte[]>> map = new HashMap<>();
		map.put(suffix, supplier);
		logVersion();
		capture(name, map);
	}
	private void log(String name, String suffix1, Supplier<byte[]> supplier1, String suffix2, Supplier<byte[]> supplier2) {
		Map<String, Supplier<byte[]>> map = new HashMap<>();
		map.put(suffix1, supplier1);
		map.put(suffix2, supplier2);
		logVersion();
		capture(name, map);
	}
}
