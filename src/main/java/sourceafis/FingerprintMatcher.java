// Part of SourceAFIS: https://sourceafis.machinezoo.com
package sourceafis;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import sourceafis.scalars.*;

public class FingerprintMatcher {
	static final int maxDistanceError = 13;
	static final double maxAngleError = Math.toRadians(10);
	final FingerprintTemplate template;
	Map<Integer, List<IndexedEdge>> edgeHash = new HashMap<>();
	FingerprintTemplate candidate;
	PriorityQueue<EdgePair> pairQueue = new PriorityQueue<>();
	PairInfo[] pairsByCandidate;
	PairInfo[] pairsByProbe;
	PairInfo[] pairList;
	int pairCount;
	public FingerprintMatcher(FingerprintTemplate template) {
		this.template = template;
		buildEdgeHash();
		pairsByProbe = new PairInfo[template.minutiae.size()];
		pairList = new PairInfo[template.minutiae.size()];
		for (int i = 0; i < pairList.length; ++i)
			pairList[i] = new PairInfo();
	}
	static class IndexedEdge {
		final EdgeShape shape;
		final int reference;
		final int neighbor;
		IndexedEdge(EdgeShape shape, int reference, int neighbor) {
			this.shape = shape;
			this.reference = reference;
			this.neighbor = neighbor;
		}
	}
	void buildEdgeHash() {
		for (int referenceMinutia = 0; referenceMinutia < template.minutiae.size(); ++referenceMinutia)
			for (int neighborMinutia = 0; neighborMinutia < template.minutiae.size(); ++neighborMinutia)
				if (referenceMinutia != neighborMinutia) {
					IndexedEdge edge = new IndexedEdge(new EdgeShape(template, referenceMinutia, neighborMinutia), referenceMinutia, neighborMinutia);
					for (int hash : shapeCoverage(edge.shape)) {
						List<IndexedEdge> list = edgeHash.get(hash);
						if (list == null)
							edgeHash.put(hash, list = new ArrayList<>());
						list.add(edge);
					}
				}
	}
	static List<Integer> shapeCoverage(EdgeShape edge) {
		int minLengthBin = (edge.length - maxDistanceError) / maxDistanceError;
		int maxLengthBin = (edge.length + maxDistanceError) / maxDistanceError;
		int angleBins = (int)Math.ceil(2 * Math.PI / maxAngleError);
		int minReferenceBin = (int)(Angle.difference(edge.referenceAngle, maxAngleError) / maxAngleError);
		int maxReferenceBin = (int)(Angle.add(edge.referenceAngle, maxAngleError) / maxAngleError);
		int endReferenceBin = (maxReferenceBin + 1) % angleBins;
		int minNeighborBin = (int)(Angle.difference(edge.neighborAngle, maxAngleError) / maxAngleError);
		int maxNeighborBin = (int)(Angle.add(edge.neighborAngle, maxAngleError) / maxAngleError);
		int endNeighborBin = (maxNeighborBin + 1) % angleBins;
		List<Integer> coverage = new ArrayList<>();
		for (int lengthBin = minLengthBin; lengthBin <= maxLengthBin; ++lengthBin)
			for (int referenceBin = minReferenceBin; referenceBin != endReferenceBin; referenceBin = (referenceBin + 1) % angleBins)
				for (int neighborBin = minNeighborBin; neighborBin != endNeighborBin; neighborBin = (neighborBin + 1) % angleBins)
					coverage.add((referenceBin << 24) + (neighborBin << 16) + lengthBin);
		return coverage;
	}
	public double match(FingerprintTemplate candidate) {
		final int maxTriedRoots = 70;
		final int maxTriedTriangles = 7538;
		this.candidate = candidate;
		int rootIndex = 0;
		int triangleIndex = 0;
		double bestScore = 0;
		for (MinutiaPair root : (Iterable<MinutiaPair>)roots()::iterator) {
			double score = tryRoot(root);
			if (score > bestScore)
				bestScore = score;
			++rootIndex;
			if (rootIndex >= maxTriedRoots)
				break;
			if (pairCount >= 3) {
				++triangleIndex;
				if (triangleIndex >= maxTriedTriangles)
					break;
			}
		}
		return bestScore;
	}
	interface ShapeFilter extends Predicate<EdgeShape> {
	}
	Stream<MinutiaPair> roots() {
		final int minEdgeLength = 58;
		final int maxEdgeLookups = 1633;
		ShapeFilter[] filters = new ShapeFilter[] {
			shape -> shape.length >= minEdgeLength,
			shape -> shape.length < minEdgeLength
		};
		class EdgeLookup {
			EdgeShape candidateEdge;
			int candidateReference;
		}
		Stream<EdgeLookup> lookups = Arrays.stream(filters)
			.flatMap(shapeFilter -> IntStream.range(1, candidate.minutiae.size()).boxed()
				.flatMap(step -> IntStream.range(0, step + 1).boxed()
					.flatMap(pass -> {
						List<Integer> candidateReferences = new ArrayList<>();
						for (int candidateReference = pass; candidateReference < candidate.minutiae.size(); candidateReference += step + 1)
							candidateReferences.add(candidateReference);
						return candidateReferences.stream();
					})
					.flatMap(candidateReference -> {
						int candidateNeighbor = (candidateReference + step) % candidate.minutiae.size();
						EdgeShape candidateEdge = new EdgeShape(candidate, candidateReference, candidateNeighbor);
						if (shapeFilter.test(candidateEdge)) {
							EdgeLookup lookup = new EdgeLookup();
							lookup.candidateEdge = candidateEdge;
							lookup.candidateReference = candidateReference;
							return Stream.of(lookup);
						}
						return null;
					})));
		return lookups.limit(maxEdgeLookups)
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
	static int hashShape(EdgeShape edge) {
		int lengthBin = edge.length / maxDistanceError;
		int referenceAngleBin = (int)(edge.referenceAngle / maxAngleError);
		int neighborAngleBin = (int)(edge.neighborAngle / maxAngleError);
		return (referenceAngleBin << 24) + (neighborAngleBin << 16) + lengthBin;
	}
	static boolean matchingShapes(EdgeShape probe, EdgeShape candidate) {
		int lengthDelta = probe.length - candidate.length;
		if (lengthDelta >= -maxDistanceError && lengthDelta <= maxDistanceError) {
			double complementaryAngleError = Angle.complementary(maxAngleError);
			double referenceDelta = Angle.difference(probe.referenceAngle, candidate.referenceAngle);
			if (referenceDelta <= maxAngleError || referenceDelta >= complementaryAngleError) {
				double neighborDelta = Angle.difference(probe.neighborAngle, candidate.neighborAngle);
				if (neighborDelta <= maxAngleError || neighborDelta >= complementaryAngleError)
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
			else if (pairsByProbe[neighbor.probe] != null && pairsByProbe[neighbor.probe].pair.candidate == neighbor.candidate) {
				++pairsByProbe[reference.probe].supportingEdges;
				++pairsByProbe[neighbor.probe].supportingEdges;
			}
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
	static List<MatchingPair> findMatchingPairs(NeighborEdge[] probeStar, NeighborEdge[] candidateStar) {
		double complementaryAngleError = Angle.complementary(maxAngleError);
		List<MatchingPair> results = new ArrayList<>();
		int start = 0;
		int end = 0;
		for (int candidateIndex = 0; candidateIndex < candidateStar.length; ++candidateIndex) {
			NeighborEdge candidateEdge = candidateStar[candidateIndex];
			while (start < probeStar.length && probeStar[start].edge.length < candidateEdge.edge.length - maxDistanceError)
				++start;
			if (end < start)
				end = start;
			while (end < probeStar.length && probeStar[end].edge.length <= candidateEdge.edge.length + maxDistanceError)
				++end;
			for (int probeIndex = start; probeIndex < end; ++probeIndex) {
				NeighborEdge probeEdge = probeStar[probeIndex];
				double referenceDiff = Angle.difference(probeEdge.edge.referenceAngle, candidateEdge.edge.referenceAngle);
				if (referenceDiff <= maxAngleError || referenceDiff >= complementaryAngleError) {
					double neighborDiff = Angle.difference(probeEdge.edge.neighborAngle, candidateEdge.edge.neighborAngle);
					if (neighborDiff <= maxAngleError || neighborDiff >= complementaryAngleError)
						results.add(new MatchingPair(new MinutiaPair(probeEdge.neighbor, candidateEdge.neighbor), candidateEdge.edge.length));
				}
			}
		}
		return results;
	}
	void skipPaired() {
		while (!pairQueue.isEmpty() && (pairsByProbe[pairQueue.peek().neighbor.probe] != null || pairsByCandidate[pairQueue.peek().neighbor.candidate] != null)) {
			EdgePair edge = pairQueue.remove();
			if (pairsByProbe[edge.neighbor.probe] != null && pairsByProbe[edge.neighbor.probe].pair.candidate == edge.neighbor.candidate) {
				++pairsByProbe[edge.reference.probe].supportingEdges;
				++pairsByProbe[edge.neighbor.probe].supportingEdges;
			}
		}
	}
	void addPair(EdgePair edge) {
		pairsByCandidate[edge.neighbor.candidate] = pairsByProbe[edge.neighbor.probe] = pairList[pairCount];
		pairList[pairCount].pair = edge.neighbor;
		pairList[pairCount].reference = edge.reference;
		++pairCount;
	}
	double computeScore() {
		final int minSupportingEdges = 1;
		final double distanceErrorFlatness = 0.69;
		final double angleErrorFlatness = 0.27;
		final double pairCountFactor = 0.032;
		final double pairFractionFactor = 8.98;
		final double correctTypeFactor = 0.629;
		final double supportedCountFactor = 0.193;
		final double edgeCountFactor = 0.265;
		final double distanceAccuracyFactor = 9.9;
		final double angleAccuracyFactor = 2.79;
		double score = pairCountFactor * pairCount;
		score += pairFractionFactor * (pairCount / (double)template.minutiae.size() + pairCount / (double)candidate.minutiae.size()) / 2;
		for (int i = 0; i < pairCount; ++i) {
			PairInfo pair = pairList[i];
			if (pair.supportingEdges >= minSupportingEdges)
				score += supportedCountFactor;
			score += edgeCountFactor * (pair.supportingEdges + 1);
			if (template.minutiae.get(pair.pair.probe).type == candidate.minutiae.get(pair.pair.candidate).type)
				score += correctTypeFactor;
		}
		int innerDistanceRadius = (int)Math.round(distanceErrorFlatness * maxDistanceError);
		int innerAngleRadius = (int)Math.round(angleErrorFlatness * maxAngleError);
		int distanceErrorSum = 0;
		int angleErrorSum = 0;
		for (int i = 1; i < pairCount; ++i) {
			PairInfo pair = pairList[i];
			EdgeShape probeEdge = new EdgeShape(template, pair.reference.probe, pair.pair.probe);
			EdgeShape candidateEdge = new EdgeShape(candidate, pair.reference.candidate, pair.pair.candidate);
			distanceErrorSum += Math.abs(probeEdge.length - candidateEdge.length);
			angleErrorSum += Math.max(innerDistanceRadius, Angle.distance(probeEdge.referenceAngle, candidateEdge.referenceAngle));
			angleErrorSum += Math.max(innerAngleRadius, Angle.distance(probeEdge.neighborAngle, candidateEdge.neighborAngle));
		}
		if (pairCount >= 2) {
			double pairedDistanceError = maxDistanceError * (pairCount - 1);
			score += distanceAccuracyFactor * (pairedDistanceError - distanceErrorSum) / pairedDistanceError;
			double pairedAngleError = maxAngleError * (pairCount - 1) * 2;
			score += angleAccuracyFactor * (pairedAngleError - angleErrorSum) / pairedAngleError;
		}
		return score;
	}
	static class MinutiaPair {
		final int probe;
		final int candidate;
		MinutiaPair(int probe, int candidate) {
			this.probe = probe;
			this.candidate = candidate;
		}
	}
	static class PairInfo {
		MinutiaPair pair;
		MinutiaPair reference;
		int supportingEdges;
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
