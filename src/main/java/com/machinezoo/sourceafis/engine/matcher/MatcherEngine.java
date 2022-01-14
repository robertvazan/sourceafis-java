// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

import com.machinezoo.sourceafis.engine.templates.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class MatcherEngine {
	private static final ThreadLocal<MatcherEngine> threads = new ThreadLocal<MatcherEngine>() {
		/*
		 * ThreadLocal has method withInitial() that is more convenient,
		 * but that method alone would force whole SourceAFIS to require Android API level 26 instead of 24.
		 */
		@Override
		protected MatcherEngine initialValue() {
			return new MatcherEngine();
		}
	};
	public static MatcherEngine current() {
		return threads.get();
	}
	private final MinutiaPairPool pool = new MinutiaPairPool();
	private final RootEnumerator roots = new RootEnumerator(pool);
	private final PairingGraph pairing = new PairingGraph(pool);
	private final EdgeSpider spider = new EdgeSpider(pool);
	private final Scoring scoring = new Scoring();
	public double match(ImmutableProbe probe, ImmutableTemplate candidate) {
		try {
			/*
			 * Thread-local storage is fairly fast, but it's still a hash lookup,
			 * so do not access TransparencySink.current() repeatedly in tight loops.
			 */
			var transparency = TransparencySink.current();
			pairing.reserveProbe(probe);
			pairing.reserveCandidate(candidate);
			/*
			 * Collection of support edges is very slow. It must be disabled on matcher level for it to have no performance impact.
			 */
			pairing.supportEnabled = transparency.acceptsPairing();
			roots.enumerate(probe, candidate);
			// https://sourceafis.machinezoo.com/transparency/root-pairs
			transparency.logRootPairs(roots.count, roots.pairs);
			double high = 0;
			int best = -1;
			for (int i = 0; i < roots.count; ++i) {
				spider.crawl(probe.template.edges, candidate.edges, pairing, roots.pairs[i]);
				// https://sourceafis.machinezoo.com/transparency/pairing
				transparency.logPairing(pairing);
				scoring.compute(probe, candidate, pairing);
				// https://sourceafis.machinezoo.com/transparency/score
				transparency.logScore(scoring);
				double partial = scoring.shapedScore;
				if (best < 0 || partial > high) {
					high = partial;
					best = i;
				}
				pairing.clear();
			}
			if (best >= 0) {
				pairing.supportEnabled = transparency.acceptsBestPairing();
				spider.crawl(probe.template.edges, candidate.edges, pairing, roots.pairs[best]);
				// https://sourceafis.machinezoo.com/transparency/pairing
				transparency.logBestPairing(pairing);
				scoring.compute(probe, candidate, pairing);
				// https://sourceafis.machinezoo.com/transparency/score
				transparency.logBestScore(scoring);
				pairing.clear();
			}
			// https://sourceafis.machinezoo.com/transparency/best-match
			transparency.logBestMatch(best);
			return high;
		} catch (Throwable e) {
			threads.remove();
			throw e;
		}
	}
}
