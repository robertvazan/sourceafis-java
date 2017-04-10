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
	}
	static class MinutiaPair {
		final int probe;
		final int candidate;
		public MinutiaPair(int probe, int candidate) {
			this.probe = probe;
			this.candidate = candidate;
		}
	}
	static class PairInfo {
		MinutiaPair pair;
		MinutiaPair reference;
		int supportingEdges;
	}
}
