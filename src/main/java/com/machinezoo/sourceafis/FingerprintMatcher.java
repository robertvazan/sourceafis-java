// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.util.*;
import com.machinezoo.sourceafis.models.*;
import gnu.trove.map.hash.*;

/**
 * Fingerprint template representation optimized for fast 1:N matching.
 * {@code FingerprintMatcher} maintains data structures that improve matching speed at the cost of some RAM.
 * It can efficiently match one probe fingerprint to many candidate fingerprints.
 * <p>
 * Probe fingerprint template is passed to {@link #FingerprintMatcher(FingerprintTemplate)}.
 * Candidate fingerprint templates are then passed one by one to {@link #match(FingerprintTemplate)}.
 * 
 * @see <a href="https://sourceafis.machinezoo.com/">SourceAFIS overview</a>
 * @see FingerprintTemplate
 */
public class FingerprintMatcher {
	private final FingerprintContext context = FingerprintContext.current();
	final FingerprintTemplate template;
	TIntObjectHashMap<List<IndexedEdge>> edgeHash = new TIntObjectHashMap<>();
	/**
	 * Create {@code FingerprintMatcher} from probe fingerprint template.
	 * Constructed {@code FingerprintMatcher} is heavy in terms of RAM footprint and CPU consumed to create it.
	 * It should be reused for multiple {@link #match(FingerprintTemplate)} calls in 1:N matching.
	 * 
	 * @param probe
	 *            fingerprint template to be matched to candidate fingerprints
	 * 
	 * @see #match(FingerprintTemplate)
	 */
	public FingerprintMatcher(FingerprintTemplate probe) {
		this.template = probe;
		buildEdgeHash();
	}
	void buildEdgeHash() {
		for (int reference = 0; reference < template.minutiae.length; ++reference)
			for (int neighbor = 0; neighbor < template.minutiae.length; ++neighbor)
				if (reference != neighbor) {
					IndexedEdge edge = new IndexedEdge(template.minutiae, reference, neighbor);
					for (int hash : shapeCoverage(edge)) {
						List<IndexedEdge> list = edgeHash.get(hash);
						if (list == null)
							edgeHash.put(hash, list = new ArrayList<>());
						list.add(edge);
					}
				}
		context.log("edge-hash", edgeHash);
	}
	List<Integer> shapeCoverage(EdgeShape edge) {
		int minLengthBin = (edge.length - context.maxDistanceError) / context.maxDistanceError;
		int maxLengthBin = (edge.length + context.maxDistanceError) / context.maxDistanceError;
		int angleBins = (int)Math.ceil(2 * Math.PI / context.maxAngleError);
		int minReferenceBin = (int)(Angle.difference(edge.referenceAngle, context.maxAngleError) / context.maxAngleError);
		int maxReferenceBin = (int)(Angle.add(edge.referenceAngle, context.maxAngleError) / context.maxAngleError);
		int endReferenceBin = (maxReferenceBin + 1) % angleBins;
		int minNeighborBin = (int)(Angle.difference(edge.neighborAngle, context.maxAngleError) / context.maxAngleError);
		int maxNeighborBin = (int)(Angle.add(edge.neighborAngle, context.maxAngleError) / context.maxAngleError);
		int endNeighborBin = (maxNeighborBin + 1) % angleBins;
		List<Integer> coverage = new ArrayList<>();
		for (int lengthBin = minLengthBin; lengthBin <= maxLengthBin; ++lengthBin)
			for (int referenceBin = minReferenceBin; referenceBin != endReferenceBin; referenceBin = (referenceBin + 1) % angleBins)
				for (int neighborBin = minNeighborBin; neighborBin != endNeighborBin; neighborBin = (neighborBin + 1) % angleBins)
					coverage.add((referenceBin << 24) + (neighborBin << 16) + lengthBin);
		return coverage;
	}
	/**
	 * Match candidate fingerprint template to this probe fingerprint and calculate similarity score.
	 * Candidate fingerprint is matched to probe fingerprint previously passed to {@link #FingerprintMatcher(FingerprintTemplate)}.
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
	 *            fingerprint template to be matched with probe fingerprint represented by this {@code FingerprintMatcher}
	 * @return similarity score between probe and candidate fingerprints
	 */
	public double match(FingerprintTemplate candidate) {
		MatchBuffer buffer = MatchBuffer.current();
		buffer.selectProbe(template.minutiae, template.edgeTable);
		buffer.selectMatcher(edgeHash);
		buffer.selectCandidate(candidate.minutiae, candidate.edgeTable);
		return buffer.match();
	}
}
