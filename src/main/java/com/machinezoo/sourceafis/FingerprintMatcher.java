// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import com.machinezoo.sourceafis.models.*;

/**
 * Fingerprint template representation optimized for fast 1:N matching.
 * {@code FingerprintMatcher} maintains data structures that improve matching speed at the cost of some RAM.
 * It can efficiently match one probe fingerprint to multiple candidate fingerprints.
 * <p>
 * Probe fingerprint template is passed to {@link #FingerprintMatcher(FingerprintTemplate)} constructor.
 * Candidate fingerprint templates are then passed one at a time to {@link #match(FingerprintTemplate)} method.
 * 
 * @see <a href="https://sourceafis.machinezoo.com/">SourceAFIS overview</a>
 * @see FingerprintTemplate
 */
public class FingerprintMatcher {
	private FingerprintContext context = FingerprintContext.current();
	final FingerprintTemplate template;
	Map<Integer, List<IndexedEdge>> edgeHash = new HashMap<>();
	FingerprintTemplate candidate;
	PriorityQueue<EdgePair> pairQueue = new PriorityQueue<>();
	PairInfo[] pairsByCandidate;
	PairInfo[] pairsByProbe;
	PairInfo[] pairList;
	int pairCount;
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
		pairsByProbe = new PairInfo[template.minutiae.size()];
		pairList = new PairInfo[template.minutiae.size()];
		for (int i = 0; i < pairList.length; ++i)
			pairList[i] = new PairInfo();
	}
	void buildEdgeHash() {
		for (int reference = 0; reference < template.minutiae.size(); ++reference)
			for (int neighbor = 0; neighbor < template.minutiae.size(); ++neighbor)
				if (reference != neighbor) {
					IndexedEdge edge = new IndexedEdge(new EdgeShape(template.minutiae.get(reference), template.minutiae.get(neighbor)), reference, neighbor);
					for (int hash : shapeCoverage(edge.shape)) {
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
	 * Match candidate fingerprint template and calculate similarity score.
	 * Candidate fingerprint is matched to probe fingerprint previously passed to {@link #FingerprintMatcher(FingerprintTemplate)} constructor.
	 * <p>
	 * Only one thread can call this method. For multi-threaded matching, create one {@code FingerprintMatcher} per thread.
	 * <p>
	 * Returned similarity score is a non-negative number that increases with similarity between probe and candidate fingerprints.
	 * Application should compare the score to a threshold with expression {@code score >= threshold} to get boolean match/non-match decision.
	 * Threshold 10 corresponds to FMR (false match rate) of 10%, threshold 20 to FMR 1%, threshold 30 to FMR 0.1%, and so on.
	 * <p>
	 * Recommended threshold is 40, which corresponds to FMR 0.01%.
	 * Correspondence between threshold and FMR is approximate and varies with quality of fingerprints being matched.
	 * Increasing threshold rapidly reduces FMR, but it also slowly increases FNMR (false non-match rate).
	 * Threshold must be tailored to the needs of the application.
	 * 
	 * @param candidate
	 *            fingerprint template to be matched with probe fingerprint represented by this {@code FingerprintMatcher}
	 * @return similarity score between probe and candidate fingerprints
	 */
	public double match(FingerprintTemplate candidate) {
		context = FingerprintContext.current();
		this.candidate = candidate;
		int rootIndex = 0;
		int triangleIndex = 0;
		double bestScore = 0;
		for (MinutiaPair root : (Iterable<MinutiaPair>)roots()::iterator) {
			context.log("tried-root", root);
			double score = tryRoot(root);
			if (score > bestScore) {
				bestScore = score;
				if (context.logging()) {
					context.log("pair-count", pairCount);
					context.log("pair-list", pairList);
				}
			}
			++rootIndex;
			if (rootIndex >= context.maxTriedRoots)
				break;
			if (pairCount >= 3) {
				++triangleIndex;
				if (triangleIndex >= context.maxTriedTriangles)
					break;
			}
		}
		return context.shapedScore ? ScoreShape.shape(bestScore) : bestScore;
	}
	interface ShapeFilter extends Predicate<EdgeShape> {
	}
	Stream<MinutiaPair> roots() {
		ShapeFilter[] filters = new ShapeFilter[] {
			shape -> shape.length >= context.minRootEdgeLength,
			shape -> shape.length < context.minRootEdgeLength
		};
		class EdgeLookup {
			EdgeShape candidateEdge;
			int candidateReference;
		}
		Stream<EdgeLookup> lookups = Arrays.stream(filters)
			.flatMap(shapeFilter -> IntStream.range(1, candidate.minutiae.size()).boxed()
				.flatMap(step -> IntStream.range(0, step + 1).boxed()
					.flatMap(pass -> {
						List<Integer> roots = new ArrayList<>();
						for (int root = pass; root < candidate.minutiae.size(); root += step + 1)
							roots.add(root);
						return roots.stream();
					})
					.flatMap(root -> {
						int neighbor = (root + step) % candidate.minutiae.size();
						EdgeShape candidateEdge = new EdgeShape(candidate.minutiae.get(root), candidate.minutiae.get(neighbor));
						if (shapeFilter.test(candidateEdge)) {
							EdgeLookup lookup = new EdgeLookup();
							lookup.candidateEdge = candidateEdge;
							lookup.candidateReference = root;
							return Stream.of(lookup);
						}
						return null;
					})));
		return lookups.limit(context.maxRootEdgeLookups)
			.flatMap(lookup -> {
				List<IndexedEdge> matches = edgeHash.get(hashShape(lookup.candidateEdge));
				if (matches != null) {
					return matches.stream()
						.filter(match -> matchingShapes(match.shape, lookup.candidateEdge))
						.map(match -> new MinutiaPair(match.reference, lookup.candidateReference));
				}
				return null;
			});
	}
	int hashShape(EdgeShape edge) {
		int lengthBin = edge.length / context.maxDistanceError;
		int referenceAngleBin = (int)(edge.referenceAngle / context.maxAngleError);
		int neighborAngleBin = (int)(edge.neighborAngle / context.maxAngleError);
		return (referenceAngleBin << 24) + (neighborAngleBin << 16) + lengthBin;
	}
	boolean matchingShapes(EdgeShape probe, EdgeShape candidate) {
		int lengthDelta = probe.length - candidate.length;
		if (lengthDelta >= -context.maxDistanceError && lengthDelta <= context.maxDistanceError) {
			double complementaryAngleError = Angle.complementary(context.maxAngleError);
			double referenceDelta = Angle.difference(probe.referenceAngle, candidate.referenceAngle);
			if (referenceDelta <= context.maxAngleError || referenceDelta >= complementaryAngleError) {
				double neighborDelta = Angle.difference(probe.neighborAngle, candidate.neighborAngle);
				if (neighborDelta <= context.maxAngleError || neighborDelta >= complementaryAngleError)
					return true;
			}
		}
		return false;
	}
	double tryRoot(MinutiaPair root) {
		createRootPairing(root);
		buildPairing();
		return computeScore();
	}
	void createRootPairing(MinutiaPair root) {
		if (pairsByCandidate == null || pairsByCandidate.length < candidate.minutiae.size())
			pairsByCandidate = new PairInfo[candidate.minutiae.size()];
		for (int i = 0; i < pairCount; ++i) {
			pairList[i].supportingEdges = 0;
			pairsByProbe[pairList[i].pair.probe] = null;
			pairsByCandidate[pairList[i].pair.candidate] = null;
		}
		pairsByCandidate[root.candidate] = pairsByProbe[root.probe] = pairList[0];
		pairList[0].pair = root;
		pairCount = 1;
	}
	void buildPairing() {
		while (true) {
			collectEdges();
			skipPaired();
			if (pairQueue.isEmpty())
				break;
			addPair(pairQueue.remove());
		}
	}
	PairInfo lastPair() {
		return pairList[pairCount - 1];
	}
	void collectEdges() {
		MinutiaPair reference = lastPair().pair;
		NeighborEdge[] probeNeighbors = template.edgeTable[reference.probe];
		NeighborEdge[] candidateNeigbors = candidate.edgeTable[reference.candidate];
		List<MatchingPair> matches = findMatchingPairs(probeNeighbors, candidateNeigbors);
		for (MatchingPair match : matches) {
			MinutiaPair neighbor = match.pair;
			if (pairsByCandidate[neighbor.candidate] == null && pairsByProbe[neighbor.probe] == null)
				pairQueue.add(new EdgePair(reference, neighbor, match.distance));
			else if (pairsByProbe[neighbor.probe] != null && pairsByProbe[neighbor.probe].pair.candidate == neighbor.candidate)
				addSupportingEdge(reference, neighbor);
		}
	}
	static class MatchingPair {
		MinutiaPair pair;
		int distance;
		MatchingPair(MinutiaPair pair, int distance) {
			this.pair = pair;
			this.distance = distance;
		}
	}
	List<MatchingPair> findMatchingPairs(NeighborEdge[] probeStar, NeighborEdge[] candidateStar) {
		double complementaryAngleError = Angle.complementary(context.maxAngleError);
		List<MatchingPair> results = new ArrayList<>();
		int start = 0;
		int end = 0;
		for (int candidateIndex = 0; candidateIndex < candidateStar.length; ++candidateIndex) {
			NeighborEdge candidateEdge = candidateStar[candidateIndex];
			while (start < probeStar.length && probeStar[start].shape.length < candidateEdge.shape.length - context.maxDistanceError)
				++start;
			if (end < start)
				end = start;
			while (end < probeStar.length && probeStar[end].shape.length <= candidateEdge.shape.length + context.maxDistanceError)
				++end;
			for (int probeIndex = start; probeIndex < end; ++probeIndex) {
				NeighborEdge probeEdge = probeStar[probeIndex];
				double referenceDiff = Angle.difference(probeEdge.shape.referenceAngle, candidateEdge.shape.referenceAngle);
				if (referenceDiff <= context.maxAngleError || referenceDiff >= complementaryAngleError) {
					double neighborDiff = Angle.difference(probeEdge.shape.neighborAngle, candidateEdge.shape.neighborAngle);
					if (neighborDiff <= context.maxAngleError || neighborDiff >= complementaryAngleError)
						results.add(new MatchingPair(new MinutiaPair(probeEdge.neighbor, candidateEdge.neighbor), candidateEdge.shape.length));
				}
			}
		}
		return results;
	}
	void skipPaired() {
		while (!pairQueue.isEmpty() && (pairsByProbe[pairQueue.peek().neighbor.probe] != null || pairsByCandidate[pairQueue.peek().neighbor.candidate] != null)) {
			EdgePair edge = pairQueue.remove();
			if (pairsByProbe[edge.neighbor.probe] != null && pairsByProbe[edge.neighbor.probe].pair.candidate == edge.neighbor.candidate)
				addSupportingEdge(edge.reference, edge.neighbor);
		}
	}
	void addPair(EdgePair edge) {
		pairsByCandidate[edge.neighbor.candidate] = pairsByProbe[edge.neighbor.probe] = pairList[pairCount];
		pairList[pairCount].pair = edge.neighbor;
		pairList[pairCount].reference = edge.reference;
		++pairCount;
	}
	void addSupportingEdge(MinutiaPair reference, MinutiaPair neighbor) {
		++pairsByProbe[reference.probe].supportingEdges;
		++pairsByProbe[neighbor.probe].supportingEdges;
		if (context.logging())
			context.log("supporting-edge", new PairInfo(neighbor, reference, 0));
	}
	double computeScore() {
		double minutiaScore = context.pairCountScore * pairCount;
		double ratioScore = context.pairFractionScore * (pairCount / (double)template.minutiae.size() + pairCount / (double)candidate.minutiae.size()) / 2;
		double supportedScore = 0;
		double edgeScore = 0;
		double typeScore = 0;
		for (int i = 0; i < pairCount; ++i) {
			PairInfo pair = pairList[i];
			if (pair.supportingEdges >= context.minSupportingEdges)
				supportedScore += context.supportedCountScore;
			edgeScore += context.edgeCountScore * (pair.supportingEdges + 1);
			if (template.minutiae.get(pair.pair.probe).type == candidate.minutiae.get(pair.pair.candidate).type)
				typeScore += context.correctTypeScore;
		}
		int innerDistanceRadius = (int)Math.round(context.distanceErrorFlatness * context.maxDistanceError);
		int innerAngleRadius = (int)Math.round(context.angleErrorFlatness * context.maxAngleError);
		int distanceErrorSum = 0;
		int angleErrorSum = 0;
		for (int i = 1; i < pairCount; ++i) {
			PairInfo pair = pairList[i];
			EdgeShape probeEdge = new EdgeShape(template.minutiae.get(pair.reference.probe), template.minutiae.get(pair.pair.probe));
			EdgeShape candidateEdge = new EdgeShape(candidate.minutiae.get(pair.reference.candidate), candidate.minutiae.get(pair.pair.candidate));
			distanceErrorSum += Math.max(innerDistanceRadius, Math.abs(probeEdge.length - candidateEdge.length));
			angleErrorSum += Math.max(innerAngleRadius, Angle.distance(probeEdge.referenceAngle, candidateEdge.referenceAngle));
			angleErrorSum += Math.max(innerAngleRadius, Angle.distance(probeEdge.neighborAngle, candidateEdge.neighborAngle));
		}
		double distanceScore = 0;
		double angleScore = 0;
		if (pairCount >= 2) {
			double pairedDistanceError = context.maxDistanceError * (pairCount - 1);
			distanceScore = context.distanceAccuracyScore * (pairedDistanceError - distanceErrorSum) / pairedDistanceError;
			double pairedAngleError = context.maxAngleError * (pairCount - 1) * 2;
			angleScore = context.angleAccuracyScore * (pairedAngleError - angleErrorSum) / pairedAngleError;
		}
		double score = minutiaScore + ratioScore + supportedScore + edgeScore + typeScore + distanceScore + angleScore;
		if (context.logging()) {
			context.log("minutia-score", minutiaScore);
			context.log("ratio-score", ratioScore);
			context.log("supported-score", supportedScore);
			context.log("edge-score", edgeScore);
			context.log("type-score", typeScore);
			context.log("distance-score", distanceScore);
			context.log("angle-score", angleScore);
			context.log("total-score", score);
		}
		return score;
	}
	static class EdgePair implements Comparable<EdgePair> {
		MinutiaPair reference;
		MinutiaPair neighbor;
		int distance;
		EdgePair(MinutiaPair reference, MinutiaPair neighbor, int distance) {
			this.reference = reference;
			this.neighbor = neighbor;
			this.distance = distance;
		}
		@Override public int compareTo(EdgePair other) {
			return Integer.compare(distance, other.distance);
		}
	}
}
