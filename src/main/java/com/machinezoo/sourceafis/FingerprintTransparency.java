// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;
import java.util.zip.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.cbor.*;
import com.machinezoo.noexception.*;
import it.unimi.dsi.fastutil.ints.*;

/**
 * Algorithm transparency API that can capture all intermediate data structures produced by SourceAFIS algorithm.
 * See <a href="https://sourceafis.machinezoo.com/transparency/">algorithm transparency</a> pages
 * on SourceAFIS website for more information and a tutorial on how to use this class.
 * <p>
 * Applications can subclass {@code FingerprintTransparency} and override
 * {@link #take(String, String, byte[])} method to define new transparency data logger.
 * One default implementation of {@code FingerprintTransparency} is returned by {@link #zip(OutputStream)} method.
 * Applications can control what transparency data gets produced by overriding {@link #accepts(String)}.
 * <p>
 * {@code FingerprintTransparency} instance should be created in a try-with-resources construct.
 * It will be capturing transparency data from all operations on current thread
 * between invocation of the constructor and invocation of {@link #close()} method,
 * which is called automatically in the try-with-resources construct.
 *
 * @see <a href="https://sourceafis.machinezoo.com/transparency/">Algorithm transparency in SourceAFIS</a>
 */
public abstract class FingerprintTransparency implements AutoCloseable {
	/*
	 * API roadmap:
	 * - log()
	 * - capture()
	 */
	static {
		PlatformCheck.run();
	}
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
	 * Filters transparency data keys that can be passed to {@link #take(String, String, byte[])}.
	 * Default implementation always returns {@code true}, i.e. all transparency data is passed to {@link #take(String, String, byte[])}.
	 * Implementation can override this method to filter some keys out, which improves performance.
	 * <p>
	 * This method should always return the same result for the same key.
	 * Result may be cached and this method might not be called every time something is about to be logged.
	 * 
	 * @param key
	 *            transparency data key as used in {@link #take(String, String, byte[])}
	 * @return whether transparency data under given key should be logged
	 * 
	 * @see #take(String, String, byte[])
	 */
	public boolean accepts(String key) {
		/*
		 * Accepting everything by default makes the API easier to use since this method can be ignored.
		 */
		return true;
	}
	/*
	 * Specifying MIME type of the data allows construction of generic transparency data consumers.
	 * For example, ZIP output for transparency data uses MIME type to assign file extension.
	 * It is also possible to create generic transparency data browser that changes visualization based on MIME type.
	 * 
	 * We will define short table mapping MIME types to file extensions, which is used by the ZIP implementation,
	 * but it is currently also used to support the old API that used file extensions.
	 * There are some MIME libraries out there, but no one was just right.
	 * There are also public MIME type lists, but they have to be bundled and then kept up to date.
	 * We will instead define only a short MIME type list covering data types we are likely to see here.
	 */
	private static String suffix(String mime) {
		switch (mime) {
		/*
		 * Our primary serialization format.
		 */
		case "application/cbor":
			return ".cbor";
		/*
		 * Plain text for simple records.
		 */
		case "text/plain":
			return ".txt";
		/*
		 * Common serialization formats.
		 */
		case "application/json":
			return ".json";
		case "application/xml":
			return ".xml";
		/*
		 * Image formats commonly used to encode fingerprints.
		 */
		case "image/jpeg":
			return ".jpeg";
		case "image/png":
			return ".png";
		case "image/bmp":
			return ".bmp";
		case "image/tiff":
			return ".tiff";
		case "image/jp2":
			return ".jp2";
		/*
		 * WSQ doesn't have a MIME type. We will invent one.
		 */
		case "image/x-wsq":
			return ".wsq";
		/*
		 * Fallback is needed, because there can be always some unexpected MIME type.
		 */
		default:
			return ".dat";
		}
	}
	/**
	 * Records transparency data. Subclasses must override this method, because the default implementation does nothing.
	 * While this {@code FingerprintTransparency} object is active (between call to the constructor and call to {@link #close()}),
	 * this method is called with transparency data in its parameters.
	 * <p>
	 * Parameter {@code key} specifies the kind of transparency data being logged,
	 * usually corresponding to some stage in the algorithm.
	 * Parameter {@code data} then contains the actual transparency data.
	 * This method may be called multiple times with the same {@code key}
	 * if the algorithm produces that kind of transparency data repeatedly.
	 * See <a href="https://sourceafis.machinezoo.com/transparency/">algorithm transparency</a>
	 * on SourceAFIS website for documentation of the structure of the transparency data.
	 * <p>
	 * Transparency data is offered only if {@link #accepts(String)} returns {@code true} for the same {@code key}.
	 * This allows applications to efficiently collect only transparency data that is actually needed.
	 * <p>
	 * MIME type of the transparency data is provided, which may be useful for generic implementations,
	 * for example transparency data browser app that changes type of visualization based on the MIME type.
	 * Most transparency data is serialized in <a href="https://cbor.io/">CBOR</a> format (MIME application/cbor).
	 * <p>
	 * Implementations of this method should be synchronized. Although the current SourceAFIS algorithm is single-threaded,
	 * future versions of SourceAFIS might run some parts of the algorithm in parallel, which would result in concurrent calls to this method.
	 * <p>
	 * If this method throws, exception is propagated through SourceAFIS code.
	 * 
	 * @param key
	 *            specifies the kind of transparency data being logged
	 * @param mime
	 *            MIME type of the transparency data in {@code data} parameter
	 * @param data
	 *            transparency data being logged
	 * 
	 * @see <a href="https://sourceafis.machinezoo.com/transparency/">Algorithm transparency in SourceAFIS</a>
	 * @see #accepts(String)
	 * @see #zip(OutputStream)
	 */
	public void take(String key, String mime, byte[] data) {
		/*
		 * If nobody overrides this method, assume it is legacy code and call the old capture() method.
		 */
		Map<String, Supplier<byte[]>> map = new HashMap<>();
		map.put(suffix(mime), () -> data);
		capture(key, map);
	}
	/**
	 * Records transparency data (deprecated).
	 * This is a deprecated variant of {@link #take(String, String, byte[])}.
	 * This method is only called if {@link #take(String, String, byte[])} is not overridden.
	 * 
	 * @param key
	 *            specifies the kind of transparency data being reported
	 * @param data
	 *            a map of suffixes (like {@code .cbor} or {@code .dat}) to {@link Supplier} of the available transparency data
	 * 
	 * @see #take(String, String, byte[])
	 * @deprecated
	 */
	@Deprecated protected void capture(String key, Map<String, Supplier<byte[]>> data) {
		/*
		 * If nobody overrode this method, assume it is legacy code and call the old log() method.
		 */
		Map<String, Supplier<ByteBuffer>> translated = new HashMap<>();
		for (Map.Entry<String, Supplier<byte[]>> entry : data.entrySet())
			translated.put(entry.getKey(), () -> ByteBuffer.wrap(entry.getValue().get()));
		log(key, translated);
	}
	/**
	 * Records transparency data (deprecated).
	 * This is a deprecated variant of {@link #take(String, String, byte[])}.
	 * This method is only called if {@link #take(String, String, byte[])} and {@link #capture(String, Map)} are not overridden.
	 * 
	 * @param key
	 *            specifies the kind of transparency data being reported
	 * @param data
	 *            a map of suffixes (like {@code .cbor} or {@code .dat}) to {@link Supplier} of the available transparency data
	 * 
	 * @see #take(String, String, byte[])
	 * @deprecated
	 */
	@Deprecated protected void log(String key, Map<String, Supplier<ByteBuffer>> data) {
	}
	private boolean closed;
	/**
	 * Deactivates transparency logging and releases system resources held by this instance if any.
	 * This method is normally called automatically when {@code FingerprintTransparency} is used in try-with-resources construct.
	 * <p>
	 * Deactivation stops transparency data logging to this instance of {@code FingerprintTransparency}.
	 * Logging thus takes place between invocation of constructor ({@link #FingerprintTransparency()}) and invocation of this method.
	 * If activations were nested, this method reactivates the outer {@code FingerprintTransparency}.
	 * <p>
	 * Subclasses can override this method to perform cleanup.
	 * Default implementation of this method performs deactivation.
	 * It must be called by overriding methods for deactivation to work correctly.
	 * <p>
	 * This method doesn't declare any checked exceptions in order to spare callers of mandatory exception handling.
	 * If your code needs to throw a checked exception, wrap it in an unchecked exception.
	 * 
	 * @see #FingerprintTransparency()
	 */
	@Override public void close() {
		/*
		 * Tolerate double call to close().
		 */
		if (!closed) {
			closed = true;
			current.set(outer);
			/*
			 * Drop reference to outer transparency object in case this instance is kept alive for too long.
			 */
			outer = null;
		}
	}
	private static class TransparencyZip extends FingerprintTransparency {
		private final ZipOutputStream zip;
		private int offset;
		TransparencyZip(OutputStream stream) {
			zip = new ZipOutputStream(stream);
		}
		/*
		 * Synchronize take(), because ZipOutputStream can be accessed only from one thread
		 * while transparency data may flow from multiple threads.
		 */
		@Override public synchronized void take(String key, String mime, byte[] data) {
			++offset;
			/*
			 * We allow providing custom output stream, which can fail at any moment.
			 * We however also offer an API that is free of checked exceptions.
			 * We will therefore wrap any checked exceptions from the output stream.
			 */
			Exceptions.wrap().run(() -> {
				zip.putNextEntry(new ZipEntry(String.format("%03d", offset) + "-" + key + suffix(mime)));
				zip.write(data);
				zip.closeEntry();
			});
		}
		@Override public void close() {
			super.close();
			Exceptions.wrap().run(zip::close);
		}
	}
	/**
	 * Writes all transparency data to a ZIP file.
	 * This is a convenience method to enable easy exploration of the available data.
	 * Programmatic processing of transparency data should be done by subclassing {@code FingerprintTransparency}
	 * and overriding {@link #take(String, String, byte[])} method.
	 * <p>
	 * ZIP file entries have filenames like {@code NNN-key.ext} where {@code NNN} is a sequentially assigned ID,
	 * {@code key} comes from {@link #take(String, String, byte[])} parameter, and {@code ext} is derived from MIME type.
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
	 * @see <a href="https://sourceafis.machinezoo.com/transparency/">Algorithm transparency in SourceAFIS</a>
	 * @see #close()
	 * @see #take(String, String, byte[])
	 */
	public static FingerprintTransparency zip(OutputStream stream) {
		return new TransparencyZip(stream);
	}
	/*
	 * To avoid null checks everywhere, we have one noop class as a fallback.
	 */
	private static final FingerprintTransparency NOOP;
	private static class NoFingerprintTransparency extends FingerprintTransparency {
		@Override public boolean accepts(String key) {
			return false;
		}
	}
	static {
		NOOP = new NoFingerprintTransparency();
		/*
		 * Deactivate logging to the noop logger as soon as it is created.
		 */
		NOOP.close();
	}
	static FingerprintTransparency current() {
		return Optional.ofNullable(current.get()).orElse(NOOP);
	}
	private static final ObjectMapper mapper = new ObjectMapper(new CBORFactory())
		.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
	private byte[] cbor(Object data) {
		return Exceptions.wrap(IllegalArgumentException::new).get(() -> mapper.writeValueAsBytes(data));
	}
	/*
	 * Use fast double-checked locking, because this could be called in tight loops.
	 */
	private volatile boolean versionOffered;
	private void logVersion() {
		if (!versionOffered) {
			boolean offer = false;
			synchronized (this) {
				if (!versionOffered) {
					versionOffered = true;
					offer = true;
				}
			}
			if (offer && accepts("version"))
				take("version", "text/plain", FingerprintCompatibility.version().getBytes(StandardCharsets.UTF_8));
		}
	}
	private void log(String key, String mime, Supplier<byte[]> supplier) {
		logVersion();
		if (accepts(key))
			take(key, mime, supplier.get());
	}
	void log(String key, Supplier<?> supplier) {
		log(key, "application/cbor", () -> cbor(supplier.get()));
	}
	void log(String key, Object data) {
		log(key, "application/cbor", () -> cbor(data));
	}
	@SuppressWarnings("unused") private static class CborSkeletonRidge {
		int start;
		int end;
		List<IntPoint> points;
	}
	@SuppressWarnings("unused") private static class CborSkeleton {
		int width;
		int height;
		List<IntPoint> minutiae;
		List<CborSkeletonRidge> ridges;
		CborSkeleton(Skeleton skeleton) {
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
						CborSkeletonRidge jr = new CborSkeletonRidge();
						jr.start = offsets.get(r.start());
						jr.end = offsets.get(r.end());
						jr.points = r.points;
						return jr;
					}))
				.collect(toList());
		}
	}
	void logSkeleton(String keyword, Skeleton skeleton) {
		log(skeleton.type.prefix + keyword, () -> new CborSkeleton(skeleton));
	}
	@SuppressWarnings("unused") private static class CborHashEntry {
		int key;
		List<IndexedEdge> edges;
	}
	// https://sourceafis.machinezoo.com/transparency/edge-hash
	void logEdgeHash(Int2ObjectMap<List<IndexedEdge>> hash) {
		log("edge-hash", () -> {
			return Arrays.stream(hash.keySet().toIntArray())
				.sorted()
				.mapToObj(key -> {
					CborHashEntry entry = new CborHashEntry();
					entry.key = key;
					entry.edges = hash.get(key);
					return entry;
				})
				.collect(toList());
		});
	}
	@SuppressWarnings("unused") private static class CborPair {
		int probe;
		int candidate;
		CborPair(int probe, int candidate) {
			this.probe = probe;
			this.candidate = candidate;
		}
		static List<CborPair> roots(int count, MinutiaPair[] roots) {
			return Arrays.stream(roots).limit(count).map(p -> new CborPair(p.probe, p.candidate)).collect(toList());
		}
	}
	/*
	 * Cache accepts() for matcher logs in volatile variables, because calling accepts() directly every time
	 * could slow down matching perceptibly due to the high number of pairings per match.
	 */
	private volatile boolean matcherOffered;
	private volatile boolean acceptsRootPairs;
	private volatile boolean acceptsPairing;
	private volatile boolean acceptsScore;
	private volatile boolean acceptsBestMatch;
	private void offerMatcher() {
		if (!matcherOffered) {
			acceptsRootPairs = accepts("root-pairs");
			acceptsPairing = accepts("pairing");
			acceptsScore = accepts("score");
			acceptsBestMatch = accepts("best-match");
			matcherOffered = true;
		}
	}
	// https://sourceafis.machinezoo.com/transparency/roots
	void logRootPairs(int count, MinutiaPair[] roots) {
		offerMatcher();
		if (acceptsRootPairs)
			log("roots", () -> CborPair.roots(count, roots));
	}
	/*
	 * Expose fast method to check whether pairing should be logged, so that we can easily skip support edge logging.
	 */
	boolean acceptsPairing() {
		offerMatcher();
		return acceptsPairing;
	}
	@SuppressWarnings("unused") private static class CborEdge {
		int probeFrom;
		int probeTo;
		int candidateFrom;
		int candidateTo;
		CborEdge(MinutiaPair pair) {
			probeFrom = pair.probeRef;
			probeTo = pair.probe;
			candidateFrom = pair.candidateRef;
			candidateTo = pair.candidate;
		}
	}
	@SuppressWarnings("unused") private static class CborPairing {
		CborPair root;
		List<CborEdge> tree;
		List<CborEdge> support;
		CborPairing(int count, MinutiaPair[] pairs, List<MinutiaPair> support) {
			root = new CborPair(pairs[0].probe, pairs[0].candidate);
			tree = Arrays.stream(pairs).limit(count).skip(1).map(CborEdge::new).collect(toList());
			this.support = support.stream().map(CborEdge::new).collect(toList());
		}
	}
	// https://sourceafis.machinezoo.com/transparency/pairing
	void logPairing(int count, MinutiaPair[] pairs, List<MinutiaPair> support) {
		offerMatcher();
		if (acceptsPairing)
			log("pairing", new CborPairing(count, pairs, support));
	}
	// https://sourceafis.machinezoo.com/transparency/score
	void logScore(Score score) {
		offerMatcher();
		if (acceptsScore)
			log("score", score);
	}
	// https://sourceafis.machinezoo.com/transparency/best-match
	void logBestMatch(int nth) {
		offerMatcher();
		if (acceptsBestMatch)
			take("best-match", "text/plain", Integer.toString(nth).getBytes(StandardCharsets.UTF_8));
	}
}
