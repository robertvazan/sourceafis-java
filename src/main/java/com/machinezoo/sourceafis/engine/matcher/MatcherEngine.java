// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

import com.machinezoo.sourceafis.engine.templates.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class MatcherEngine {
	public static double match(ImmutableProbe probe, ImmutableTemplate candidate) {
		/*
		 * Thread-local storage is fairly fast, but it's still a hash lookup,
		 * so do not access TransparencySink.current() repeatedly in tight loops.
		 */
		var transparency = TransparencySink.current();
		var thread = MatcherThread.current();
		try {
			thread.pairing.reserveProbe(probe);
			thread.pairing.reserveCandidate(candidate);
			/*
			 * Collection of support edges is very slow. It must be disabled on matcher level for it to have no performance impact.
			 */
			thread.pairing.supportEnabled = transparency.acceptsPairing();
			RootEnumerator.enumerate(probe, candidate, thread.roots);
			// https://sourceafis.machinezoo.com/transparency/roots
			transparency.logRootPairs(thread.roots.count, thread.roots.pairs);
			double high = 0;
			int best = -1;
			for (int i = 0; i < thread.roots.count; ++i) {
				EdgeSpider.crawl(probe.template.edges, candidate.edges, thread.pairing, thread.roots.pairs[i], thread.queue);
				// https://sourceafis.machinezoo.com/transparency/pairing
				transparency.logPairing(thread.pairing);
				Scoring.compute(probe.template, candidate, thread.pairing, thread.score);
				// https://sourceafis.machinezoo.com/transparency/score
				transparency.logScore(thread.score);
				double partial = thread.score.shapedScore;
				if (best < 0 || partial > high) {
					high = partial;
					best = i;
				}
				thread.pairing.clear();
			}
			if (best >= 0) {
				thread.pairing.supportEnabled = transparency.acceptsBestPairing();
				EdgeSpider.crawl(probe.template.edges, candidate.edges, thread.pairing, thread.roots.pairs[best], thread.queue);
				// https://sourceafis.machinezoo.com/transparency/pairing
				transparency.logBestPairing(thread.pairing);
				Scoring.compute(probe.template, candidate, thread.pairing, thread.score);
				// https://sourceafis.machinezoo.com/transparency/score
				transparency.logBestScore(thread.score);
				thread.pairing.clear();
			}
			thread.roots.discard();
			// https://sourceafis.machinezoo.com/transparency/best-match
			transparency.logBestMatch(best);
			return high;
		} catch (Throwable ex) {
			MatcherThread.kill();
			throw ex;
		}
	}
}
