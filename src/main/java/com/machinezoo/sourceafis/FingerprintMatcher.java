// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.util.*;
import gnu.trove.map.hash.*;

/**
 * Fingerprint template representation optimized for fast 1:N matching.
 * {@code FingerprintMatcher} maintains data structures that improve matching speed at the cost of some RAM.
 * It can efficiently match one probe fingerprint to many candidate fingerprints.
 * <p>
 * New matcher is created by passing probe fingerprint template to {@link #index(FingerprintTemplate)}
 * on an empty fingerprint matcher instantiated  with {@link #FingerprintMatcher()} constructor.
 * Candidate fingerprint templates are then passed one by one to {@link #match(FingerprintTemplate)}.
 * 
 * @see <a href="https://sourceafis.machinezoo.com/">SourceAFIS overview</a>
 * @see FingerprintTemplate
 */
public class FingerprintMatcher {
	private FingerprintTransparency transparency = FingerprintTransparency.none;
	private volatile ImmutableMatcher immutable = ImmutableMatcher.empty;
	/**
	 * Instantiate an empty fingerprint matcher.
	 * Empty matcher does not match any {@link FingerprintTemplate} passed to {@link #match(FingerprintTemplate)}.
	 * You can call {@link #index(FingerprintTemplate)} to index probe fingerprint
	 * and {@link #match(FingerprintTemplate)} to match it to some candidate fingerprint.
	 * 
	 * @see #index(FingerprintTemplate)
	 */
	public FingerprintMatcher() {
	}
	/**
	 * Enable algorithm transparency.
	 * Subsequent operations on this matcher will report intermediate data structures created by the algorithm
	 * to the provided {@link FingerprintTransparency} instance.
	 * 
	 * @param transparency
	 *            target {@link FingerprintTransparency} or {@code null} to disable algorithm transparency
	 * @return {@code this} (fluent method)
	 * 
	 * @see FingerprintTransparency
	 */
	public FingerprintMatcher transparency(FingerprintTransparency transparency) {
		this.transparency = Optional.ofNullable(transparency).orElse(FingerprintTransparency.none);
		return this;
	}
	/**
	 * Build search data structures over probe fingerprint template.
	 * Once this method is called, it is possible to call {@link #match(FingerprintTemplate)} to compare fingerprints.
	 * <p>
	 * This method is heavy in terms of RAM footprint and CPU usage.
	 * Initialized {@code FingerprintMatcher} should be reused for multiple {@link #match(FingerprintTemplate)} calls in 1:N matching.
	 * 
	 * @param probe
	 *            probe fingerprint template to be matched to candidate fingerprints
	 * @return {@code this} (fluent method)
	 * 
	 * @see #match(FingerprintTemplate)
	 */
	public FingerprintMatcher index(FingerprintTemplate probe) {
		ImmutableTemplate template = probe.immutable;
		immutable = new ImmutableMatcher(template, buildEdgeHash(template));
		return this;
	}
	private TIntObjectHashMap<List<IndexedEdge>> buildEdgeHash(ImmutableTemplate template) {
		TIntObjectHashMap<List<IndexedEdge>> map = new TIntObjectHashMap<>();
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
		transparency.logEdgeHash(map);
		return map;
	}
	private List<Integer> shapeCoverage(EdgeShape edge) {
		int minLengthBin = (edge.length - Parameters.maxDistanceError) / Parameters.maxDistanceError;
		int maxLengthBin = (edge.length + Parameters.maxDistanceError) / Parameters.maxDistanceError;
		int angleBins = (int)Math.ceil(2 * Math.PI / Parameters.maxAngleError);
		int minReferenceBin = (int)(DoubleAngle.difference(edge.referenceAngle, Parameters.maxAngleError) / Parameters.maxAngleError);
		int maxReferenceBin = (int)(DoubleAngle.add(edge.referenceAngle, Parameters.maxAngleError) / Parameters.maxAngleError);
		int endReferenceBin = (maxReferenceBin + 1) % angleBins;
		int minNeighborBin = (int)(DoubleAngle.difference(edge.neighborAngle, Parameters.maxAngleError) / Parameters.maxAngleError);
		int maxNeighborBin = (int)(DoubleAngle.add(edge.neighborAngle, Parameters.maxAngleError) / Parameters.maxAngleError);
		int endNeighborBin = (maxNeighborBin + 1) % angleBins;
		List<Integer> coverage = new ArrayList<>();
		for (int lengthBin = minLengthBin; lengthBin <= maxLengthBin; ++lengthBin)
			for (int referenceBin = minReferenceBin; referenceBin != endReferenceBin; referenceBin = (referenceBin + 1) % angleBins)
				for (int neighborBin = minNeighborBin; neighborBin != endNeighborBin; neighborBin = (neighborBin + 1) % angleBins)
					coverage.add((referenceBin << 24) + (neighborBin << 16) + lengthBin);
		return coverage;
	}
	/**
	 * Match candidate fingerprint to probe fingerprint and calculate similarity score.
	 * Candidate fingerprint in {@code candidate} parameter is matched to probe fingerprint previously passed to {@link #index(FingerprintTemplate)}.
	 * <p>
	 * Returned similarity score is a non-negative number that increases with similarity between probe and candidate fingerprints.
	 * Application should compare the score to a threshold with expression {@code (score >= threshold)} to arrive at boolean match/non-match decision.
	 * Threshold 10 corresponds to FMR (false match rate) of 10%, threshold 20 to FMR 1%, threshold 30 to FMR 0.1%, and so on.
	 * <p>
	 * Recommended threshold is 40, which corresponds to FMR 0.01%.
	 * Correspondence between threshold and FMR is approximate and varies with quality of fingerprints being matched.
	 * Increasing threshold rapidly reduces FMR, but it also slowly increases FNMR (false non-match rate).
	 * Threshold must be tailored to the needs of the application.
	 * <p>
	 * This method is thread-safe. Multiple threads can match candidates against single {@code FingerprintMatcher}.
	 * 
	 * @param candidate
	 *            fingerprint template to be matched with probe fingerprint indexed by this {@code FingerprintMatcher}
	 * @return similarity score between probe and candidate fingerprints
	 * 
	 * @see #index(FingerprintTemplate)
	 */
	public double match(FingerprintTemplate candidate) {
		MatchBuffer buffer = MatchBuffer.current();
		try {
			buffer.transparency = transparency;
			buffer.selectMatcher(immutable);
			buffer.selectCandidate(candidate.immutable);
			return buffer.match();
		} finally {
			buffer.transparency = FingerprintTransparency.none;
		}
	}
}
