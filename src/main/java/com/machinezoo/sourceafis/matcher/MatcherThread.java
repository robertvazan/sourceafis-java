// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.matcher;

import java.util.*;
import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.templates.*;
import com.machinezoo.sourceafis.transparency.*;
import it.unimi.dsi.fastutil.ints.*;

public class MatcherThread {
	private static final ThreadLocal<MatcherThread> threads = new ThreadLocal<MatcherThread>() {
		/*
		 * ThreadLocal has method withInitial() that is more convenient,
		 * but that method alone would force whole SourceAFIS to require Android API level 26 instead of 24.
		 */
		@Override
		protected MatcherThread initialValue() {
			return new MatcherThread();
		}
	};
	TransparencySink transparency;
	ImmutableTemplate probe;
	Int2ObjectMap<List<IndexedEdge>> edgeHash;
	ImmutableTemplate candidate;
	private MinutiaPair[] pool = new MinutiaPair[1];
	private int pooled;
	PriorityQueue<MinutiaPair> queue = new PriorityQueue<>(Comparator.comparing(p -> p.distance));
	int count;
	MinutiaPair[] tree = new MinutiaPair[1];
	MinutiaPair[] byProbe = new MinutiaPair[1];
	MinutiaPair[] byCandidate = new MinutiaPair[1];
	MinutiaPair[] roots = new MinutiaPair[1];
	final IntSet duplicates = new IntOpenHashSet();
	Score score = new Score();
	final List<MinutiaPair> support = new ArrayList<>();
	boolean reportSupport;
	public static MatcherThread current() {
		return threads.get();
	}
	public void selectMatcher(ImmutableMatcher matcher) {
		probe = matcher.template;
		if (probe.minutiae.length > tree.length) {
			tree = new MinutiaPair[probe.minutiae.length];
			byProbe = new MinutiaPair[probe.minutiae.length];
		}
		edgeHash = matcher.edgeHash;
	}
	public void selectCandidate(ImmutableTemplate template) {
		candidate = template;
		if (byCandidate.length < candidate.minutiae.length)
			byCandidate = new MinutiaPair[candidate.minutiae.length];
	}
	public double match() {
		try {
			/*
			 * Thread-local storage is fairly fast, but it's still a hash lookup,
			 * so do not access FingerprintTransparency.current() repeatedly in tight loops.
			 */
			transparency = TransparencySink.current();
			/*
			 * Collection of support edges is very slow. It must be disabled on matcher level for it to have no performance impact.
			 */
			reportSupport = transparency.acceptsPairing();
			int totalRoots = RootEnumerator.enumerate(this);
			// https://sourceafis.machinezoo.com/transparency/root-pairs
			transparency.logRootPairs(totalRoots, roots);
			double high = 0;
			int best = -1;
			for (int i = 0; i < totalRoots; ++i) {
				double partial = EdgeSpider.tryRoot(this, roots[i]);
				if (best < 0 || partial > high) {
					high = partial;
					best = i;
				}
				EdgeSpider.clearPairing(this);
			}
			// https://sourceafis.machinezoo.com/transparency/best-match
			transparency.logBestMatch(best);
			return high;
		} catch (Throwable e) {
			threads.remove();
			throw e;
		} finally {
			transparency = null;
		}
	}
	MinutiaPair allocate() {
		if (pooled > 0) {
			--pooled;
			MinutiaPair pair = pool[pooled];
			pool[pooled] = null;
			return pair;
		} else
			return new MinutiaPair();
	}
	void release(MinutiaPair pair) {
		if (pooled >= pool.length)
			pool = Arrays.copyOf(pool, 2 * pool.length);
		pair.probe = 0;
		pair.candidate = 0;
		pair.probeRef = 0;
		pair.candidateRef = 0;
		pair.distance = 0;
		pair.supportingEdges = 0;
		pool[pooled] = pair;
	}
}
