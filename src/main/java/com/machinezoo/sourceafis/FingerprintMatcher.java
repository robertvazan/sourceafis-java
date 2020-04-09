// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.util.*;
import it.unimi.dsi.fastutil.ints.*;

/**
 * Fingerprint template representation optimized for fast 1:N matching.
 * {@code FingerprintMatcher} maintains data structures that improve matching speed at the cost of some RAM.
 * It can efficiently match one probe fingerprint to many candidate fingerprints.
 * <p>
 * New matcher is created by passing probe fingerprint template to {@link #index(FingerprintTemplate)}
 * on an empty fingerprint matcher instantiated with {@link #FingerprintMatcher()} constructor.
 * Candidate fingerprint templates are then passed one by one to {@link #match(FingerprintTemplate)}.
 * 
 * @see <a href="https://sourceafis.machinezoo.com/">SourceAFIS overview</a>
 * @see FingerprintTemplate
 */
public class FingerprintMatcher {
	/*
	 * API roadmap:
	 * + FingerprintMatcher(FingerprintTemplate)
	 * - index(FingerprintTemplate)
	 * + FingerprintMatcher(FingerprintTemplate, FingerprintMatcherOptions)
	 * + compare(FingerprintTemplate) - returns bits of evidence instead of current score
	 * - match(FingerprintTemplate)
	 * + maybe features to support 1:N identification (parallelization, score adjustment, person model, ...)
	 * 
	 * FingerprintMatcherOptions:
	 * + matchX(boolean) - enable or disable various parts of the matcher for performance reasons
	 * + cpu(long) - automated feature/algorithm selection to target CPU cycles per candidate
	 * 
	 * FingerprintEvidence:
	 * = calculation of effective score in multi-finger or 1:N matching
	 * + add(double)
	 * + add(FingerprintPosition, double)
	 * + add(FingerprintEvidence)
	 * + top(int subset, int population)
	 * + sum()
	 * + thresholdAtFMR(double) - might have variant, still unclear
	 */
	private volatile ImmutableMatcher immutable = ImmutableMatcher.NULL;
	/**
	 * Instantiates an empty fingerprint matcher.
	 * Empty matcher does not match any {@link FingerprintTemplate} passed to {@link #match(FingerprintTemplate)}.
	 * You can call {@link #index(FingerprintTemplate)} to index probe fingerprint
	 * and {@link #match(FingerprintTemplate)} to match it to some candidate fingerprint.
	 * 
	 * @see #index(FingerprintTemplate)
	 */
	public FingerprintMatcher() {
	}
	/**
	 * Enables algorithm transparency.
	 * Since {@link FingerprintTransparency} is activated automatically via thread-local variable
	 * in recent versions of SourceAFIS, this method does nothing in current version of SourceAFIS.
	 * It will be removed in some later version.
	 * 
	 * @param transparency
	 *            target {@link FingerprintTransparency} or {@code null} to disable algorithm transparency
	 * @return {@code this} (fluent method)
	 * 
	 * @see FingerprintTransparency
	 */
	@Deprecated public FingerprintMatcher transparency(FingerprintTransparency transparency) {
		return this;
	}
	/**
	 * Builds search data structures over probe fingerprint template.
	 * Once this method is called, it is possible to call {@link #match(FingerprintTemplate)} to compare fingerprints.
	 * <p>
	 * This method is heavy in terms of RAM footprint and CPU usage.
	 * Initialized {@code FingerprintMatcher} should be reused for multiple {@link #match(FingerprintTemplate)} calls in 1:N matching.
	 * 
	 * @param probe
	 *            probe fingerprint template to be matched to candidate fingerprints
	 * @return {@code this} (fluent method)
	 * @throws NullPointerException
	 *             if {@code probe} is {@code null}
	 * 
	 * @see #match(FingerprintTemplate)
	 */
	public FingerprintMatcher index(FingerprintTemplate probe) {
		Objects.requireNonNull(probe);
		ImmutableTemplate template = probe.immutable;
		immutable = new ImmutableMatcher(template, buildEdgeHash(template));
		return this;
	}
	private Int2ObjectMap<List<IndexedEdge>> buildEdgeHash(ImmutableTemplate template) {
		Int2ObjectMap<List<IndexedEdge>> map = new Int2ObjectOpenHashMap<>();
		for (int reference = 0; reference < template.minutiae.length; ++reference)
			for (int neighbor = 0; neighbor < template.minutiae.length; ++neighbor)
				if (reference != neighbor) {
					IndexedEdge edge = new IndexedEdge(template.minutiae, reference, neighbor);
					for (int hash : shapeCoverage(edge)) {
						List<IndexedEdge> list = map.get(hash);
						if (list == null)
							map.put(hash, list = new ArrayList<>());
						list.add(edge);
					}
				}
		// https://sourceafis.machinezoo.com/transparency/edge-hash
		FingerprintTransparency.current().logEdgeHash(map);
		return map;
	}
	private List<Integer> shapeCoverage(EdgeShape edge) {
		int minLengthBin = (edge.length - Parameters.MAX_DISTANCE_ERROR) / Parameters.MAX_DISTANCE_ERROR;
		int maxLengthBin = (edge.length + Parameters.MAX_DISTANCE_ERROR) / Parameters.MAX_DISTANCE_ERROR;
		int angleBins = (int)Math.ceil(2 * Math.PI / Parameters.MAX_ANGLE_ERROR);
		int minReferenceBin = (int)(DoubleAngle.difference(edge.referenceAngle, Parameters.MAX_ANGLE_ERROR) / Parameters.MAX_ANGLE_ERROR);
		int maxReferenceBin = (int)(DoubleAngle.add(edge.referenceAngle, Parameters.MAX_ANGLE_ERROR) / Parameters.MAX_ANGLE_ERROR);
		int endReferenceBin = (maxReferenceBin + 1) % angleBins;
		int minNeighborBin = (int)(DoubleAngle.difference(edge.neighborAngle, Parameters.MAX_ANGLE_ERROR) / Parameters.MAX_ANGLE_ERROR);
		int maxNeighborBin = (int)(DoubleAngle.add(edge.neighborAngle, Parameters.MAX_ANGLE_ERROR) / Parameters.MAX_ANGLE_ERROR);
		int endNeighborBin = (maxNeighborBin + 1) % angleBins;
		List<Integer> coverage = new ArrayList<>();
		for (int lengthBin = minLengthBin; lengthBin <= maxLengthBin; ++lengthBin)
			for (int referenceBin = minReferenceBin; referenceBin != endReferenceBin; referenceBin = (referenceBin + 1) % angleBins)
				for (int neighborBin = minNeighborBin; neighborBin != endNeighborBin; neighborBin = (neighborBin + 1) % angleBins)
					coverage.add((referenceBin << 24) + (neighborBin << 16) + lengthBin);
		return coverage;
	}
	/**
	 * Matches candidate fingerprint to probe fingerprint and calculates similarity score.
	 * Candidate fingerprint in {@code candidate} parameter is matched to probe fingerprint previously passed to {@link #index(FingerprintTemplate)}.
	 * <p>
	 * Returned similarity score is a non-negative number that increases with similarity between probe and candidate fingerprints.
	 * Application should compare the score to a threshold with expression {@code (score >= threshold)} to arrive at boolean match/non-match decision.
	 * Threshold 10 corresponds to FMR (False Match Rate, see <a href="https://en.wikipedia.org/wiki/Biometrics#Performance">Biometric Performance</a>
	 * and <a href="https://en.wikipedia.org/wiki/Confusion_matrix">Confusion matrix</a>) of 10%, threshold 20 to FMR 1%, threshold 30 to FMR 0.1%, and so on.
	 * <p>
	 * Recommended threshold is 40, which corresponds to FMR 0.01%.
	 * Correspondence between threshold and FMR is approximate and varies with quality of fingerprints being matched.
	 * Increasing threshold rapidly reduces FMR, but it also slowly increases FNMR (False Non-Match Rate).
	 * Threshold must be tailored to the needs of the application.
	 * <p>
	 * This method is thread-safe. Multiple threads can match candidates against single {@code FingerprintMatcher}.
	 * 
	 * @param candidate
	 *            fingerprint template to be matched with probe fingerprint indexed by this {@code FingerprintMatcher}
	 * @return similarity score between probe and candidate fingerprints
	 * @throws NullPointerException
	 *             if {@code candidate} is {@code null}
	 * 
	 * @see #index(FingerprintTemplate)
	 */
	public double match(FingerprintTemplate candidate) {
		Objects.requireNonNull(candidate);
		MatcherThread thread = MatcherThread.current();
		thread.selectMatcher(immutable);
		thread.selectCandidate(candidate.immutable);
		return thread.match();
	}
}
