// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.transparency;

import static java.util.stream.Collectors.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.cbor.*;
import com.machinezoo.noexception.*;
import com.machinezoo.sourceafis.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.matcher.*;
import it.unimi.dsi.fastutil.ints.*;

public class TransparencySink implements CloseableScope {
	/*
	 * Having transparency objects tied to current thread spares us of contaminating all classes with transparency APIs.
	 * Transparency object is activated on the thread the moment it is created.
	 * Having no explicit activation makes for a bit simpler API.
	 */
	private static final ThreadLocal<TransparencySink> current = new ThreadLocal<>();
	private TransparencySink outer;
	private final FingerprintTransparency transparency;
	public TransparencySink(FingerprintTransparency transparency) {
		this.transparency = transparency;
		outer = current.get();
		current.set(this);
	}
	private boolean closed;
	@Override
	public void close() {
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
	public static TransparencySink current() {
		return Optional.ofNullable(current.get()).orElse(NoTransparency.SINK);
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
			if (offer && transparency.accepts("version"))
				transparency.take("version", "text/plain", FingerprintCompatibility.version().getBytes(StandardCharsets.UTF_8));
		}
	}
	private void log(String key, String mime, Supplier<byte[]> supplier) {
		logVersion();
		if (transparency.accepts(key))
			transparency.take(key, mime, supplier.get());
	}
	public void log(String key, Supplier<?> supplier) {
		log(key, "application/cbor", () -> cbor(supplier.get()));
	}
	public void log(String key, Object data) {
		log(key, "application/cbor", () -> cbor(data));
	}
	public void logSkeleton(String keyword, Skeleton skeleton) {
		log(skeleton.type.prefix + keyword, () -> new ConsistentSkeleton(skeleton));
	}
	// https://sourceafis.machinezoo.com/transparency/edge-hash
	public void logEdgeHash(Int2ObjectMap<List<IndexedEdge>> hash) {
		log("edge-hash", () -> {
			return Arrays.stream(hash.keySet().toIntArray())
				.sorted()
				.mapToObj(key -> {
					ConsistentHashEntry entry = new ConsistentHashEntry();
					entry.key = key;
					entry.edges = hash.get(key);
					return entry;
				})
				.collect(toList());
		});
	}
	/*
	 * Cache accepts() for matcher logs in volatile variables, because calling accepts() directly every time
	 * could slow down matching perceptibly due to the high number of pairings per match.
	 */
	private volatile boolean matcherOffered;
	private volatile boolean acceptsRootPairs;
	private volatile boolean acceptsPairing;
	private volatile boolean acceptsBestPairing;
	private volatile boolean acceptsScore;
	private volatile boolean acceptsBestScore;
	private volatile boolean acceptsBestMatch;
	private void offerMatcher() {
		if (!matcherOffered) {
			acceptsRootPairs = transparency.accepts("root-pairs");
			acceptsPairing = transparency.accepts("pairing");
			acceptsBestPairing = transparency.accepts("best-pairing");
			acceptsScore = transparency.accepts("score");
			acceptsBestScore = transparency.accepts("best-score");
			acceptsBestMatch = transparency.accepts("best-match");
			matcherOffered = true;
		}
	}
	// https://sourceafis.machinezoo.com/transparency/roots
	public void logRootPairs(int count, MinutiaPair[] roots) {
		offerMatcher();
		if (acceptsRootPairs)
			log("roots", () -> ConsistentMinutiaPair.roots(count, roots));
	}
	/*
	 * Expose fast method to check whether pairing should be logged, so that we can easily skip support edge logging.
	 */
	public boolean acceptsPairing() {
		offerMatcher();
		return acceptsPairing;
	}
	public boolean acceptsBestPairing() {
		offerMatcher();
		return acceptsBestPairing;
	}
	// https://sourceafis.machinezoo.com/transparency/pairing
	public void logPairing(PairingGraph pairing) {
		offerMatcher();
		if (acceptsPairing)
			log("pairing", new ConsistentPairingGraph(pairing));
	}
	public void logBestPairing(PairingGraph pairing) {
		offerMatcher();
		if (acceptsBestPairing)
			log("best-pairing", new ConsistentPairingGraph(pairing));
	}
	// https://sourceafis.machinezoo.com/transparency/score
	public void logScore(Scoring score) {
		offerMatcher();
		if (acceptsScore)
			log("score", score);
	}
	public void logBestScore(Scoring score) {
		offerMatcher();
		if (acceptsBestScore)
			log("best-score", score);
	}
	// https://sourceafis.machinezoo.com/transparency/best-match
	public void logBestMatch(int nth) {
		offerMatcher();
		if (acceptsBestMatch)
			transparency.take("best-match", "text/plain", Integer.toString(nth).getBytes(StandardCharsets.UTF_8));
	}
}
