package com.machinezoo.sourceafis.models;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class MatchBuffer {
	private static final ThreadLocal<MatchBuffer> local = ThreadLocal.withInitial(MatchBuffer::new);
	private FingerprintContext context;
	private List<FingerprintMinutia> probeMinutiae;
	private NeighborEdge[][] probeEdges;
	private Map<Integer, List<IndexedEdge>> edgeHash;
	private List<FingerprintMinutia> candidateMinutiae;
	private NeighborEdge[][] candidateEdges;
	private PriorityQueue<EdgePair> pairQueue = new PriorityQueue<>();
	private PairInfo[] pairsByCandidate;
	private PairInfo[] pairsByProbe;
	private PairInfo[] pairList;
	private int pairCount;
	public static MatchBuffer current() {
		return local.get();
	}
	public void selectProbe(List<FingerprintMinutia> minutiae, NeighborEdge[][] edges) {
		probeMinutiae = minutiae;
		probeEdges = edges;
		if (pairList == null || minutiae.size() > pairList.length) {
			pairList = new PairInfo[minutiae.size()];
			for (int i = 0; i < pairList.length; ++i)
				pairList[i] = new PairInfo();
			pairsByProbe = new PairInfo[minutiae.size()];
		}
	}
	public void selectMatcher(Map<Integer, List<IndexedEdge>> edgeHash) {
		this.edgeHash = edgeHash;
	}
	public void selectCandidate(List<FingerprintMinutia> minutiae, NeighborEdge[][] edges) {
		candidateMinutiae = minutiae;
		candidateEdges = edges;
		if (pairsByCandidate == null || pairsByCandidate.length < minutiae.size())
			pairsByCandidate = new PairInfo[minutiae.size()];
	}
	public double match() {
		try {
			context = FingerprintContext.current();
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
		} catch (Throwable e) {
			local.remove();
			throw e;
		}
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
			.flatMap(shapeFilter -> IntStream.range(1, candidateMinutiae.size()).boxed()
				.flatMap(step -> IntStream.range(0, step + 1).boxed()
					.flatMap(pass -> {
						List<Integer> roots = new ArrayList<>();
						for (int root = pass; root < candidateMinutiae.size(); root += step + 1)
							roots.add(root);
						return roots.stream();
					})
					.flatMap(root -> {
						int neighbor = (root + step) % candidateMinutiae.size();
						EdgeShape candidateEdge = new EdgeShape(candidateMinutiae.get(root), candidateMinutiae.get(neighbor));
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
		double score = computeScore();
		clearPairing();
		return score;
	}
	void createRootPairing(MinutiaPair root) {
		pairsByCandidate[root.candidate] = pairsByProbe[root.probe] = pairList[0];
		pairList[0].pair = root;
		pairCount = 1;
	}
	void clearPairing() {
		for (int i = 0; i < pairCount; ++i) {
			pairsByProbe[pairList[i].pair.probe] = null;
			pairsByCandidate[pairList[i].pair.candidate] = null;
			pairList[i].pair = null;
			pairList[i].reference = null;
			pairList[i].supportingEdges = 0;
		}
		pairCount = 0;
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
		NeighborEdge[] probeNeighbors = probeEdges[reference.probe];
		NeighborEdge[] candidateNeigbors = candidateEdges[reference.candidate];
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
		double ratioScore = context.pairFractionScore * (pairCount / (double)probeMinutiae.size() + pairCount / (double)candidateMinutiae.size()) / 2;
		double supportedScore = 0;
		double edgeScore = 0;
		double typeScore = 0;
		for (int i = 0; i < pairCount; ++i) {
			PairInfo pair = pairList[i];
			if (pair.supportingEdges >= context.minSupportingEdges)
				supportedScore += context.supportedCountScore;
			edgeScore += context.edgeCountScore * (pair.supportingEdges + 1);
			if (probeMinutiae.get(pair.pair.probe).type == candidateMinutiae.get(pair.pair.candidate).type)
				typeScore += context.correctTypeScore;
		}
		int innerDistanceRadius = (int)Math.round(context.distanceErrorFlatness * context.maxDistanceError);
		int innerAngleRadius = (int)Math.round(context.angleErrorFlatness * context.maxAngleError);
		int distanceErrorSum = 0;
		int angleErrorSum = 0;
		for (int i = 1; i < pairCount; ++i) {
			PairInfo pair = pairList[i];
			EdgeShape probeEdge = new EdgeShape(probeMinutiae.get(pair.reference.probe), probeMinutiae.get(pair.pair.probe));
			EdgeShape candidateEdge = new EdgeShape(candidateMinutiae.get(pair.reference.candidate), candidateMinutiae.get(pair.pair.candidate));
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
