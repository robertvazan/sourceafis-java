// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.util.*;
import gnu.trove.map.hash.*;

class MatchBuffer {
	private static final ThreadLocal<MatchBuffer> local = ThreadLocal.withInitial(MatchBuffer::new);
	private FingerprintTransparency logger = FingerprintTransparency.none;
	private Minutia[] probeMinutiae;
	private NeighborEdge[][] probeEdges;
	private TIntObjectHashMap<List<IndexedEdge>> edgeHash;
	private Minutia[] candidateMinutiae;
	private NeighborEdge[][] candidateEdges;
	private MinutiaPair[] pool = new MinutiaPair[1];
	private int pooled;
	private PriorityQueue<MinutiaPair> queue = new PriorityQueue<>(Comparator.comparing(p -> p.distance));
	private int count;
	private MinutiaPair[] tree;
	private MinutiaPair[] byProbe;
	private MinutiaPair[] byCandidate;
	private MinutiaPair[] roots;
	static MatchBuffer current() {
		return local.get();
	}
	void selectProbe(Minutia[] minutiae, NeighborEdge[][] edges) {
		probeMinutiae = minutiae;
		probeEdges = edges;
		if (tree == null || minutiae.length > tree.length) {
			tree = new MinutiaPair[minutiae.length];
			byProbe = new MinutiaPair[minutiae.length];
		}
	}
	void selectMatcher(TIntObjectHashMap<List<IndexedEdge>> edgeHash) {
		this.edgeHash = edgeHash;
	}
	void selectCandidate(Minutia[] minutiae, NeighborEdge[][] edges) {
		candidateMinutiae = minutiae;
		candidateEdges = edges;
		if (byCandidate == null || byCandidate.length < minutiae.length)
			byCandidate = new MinutiaPair[minutiae.length];
	}
	double match() {
		try {
			logger = FingerprintTransparency.current();
			int totalRoots = enumerateRoots();
			logger.logRoots(totalRoots, roots);
			double high = 0;
			int best = -1;
			for (int i = 0; i < totalRoots; ++i) {
				double score = tryRoot(roots[i]);
				logger.logPairing(i, count, tree);
				if (score > high) {
					high = score;
					best = i;
				}
				clearPairing();
			}
			logger.logBestPairing(best);
			return high;
		} catch (Throwable e) {
			local.remove();
			throw e;
		} finally {
			logger = FingerprintTransparency.none;
		}
	}
	private int enumerateRoots() {
		if (roots == null || roots.length < Parameters.maxTriedRoots)
			roots = new MinutiaPair[Parameters.maxTriedRoots];
		int totalLookups = 0;
		int totalRoots = 0;
		for (boolean shortEdges : new boolean[] { false, true }) {
			for (int period = 1; period < candidateMinutiae.length; ++period) {
				for (int phase = 0; phase <= period; ++phase) {
					for (int candidateReference = phase; candidateReference < candidateMinutiae.length; candidateReference += period + 1) {
						int candidateNeighbor = (candidateReference + period) % candidateMinutiae.length;
						EdgeShape candidateEdge = new EdgeShape(candidateMinutiae[candidateReference], candidateMinutiae[candidateNeighbor]);
						if ((candidateEdge.length >= Parameters.minRootEdgeLength) ^ shortEdges) {
							List<IndexedEdge> matches = edgeHash.get(hashShape(candidateEdge));
							if (matches != null) {
								for (IndexedEdge match : matches) {
									if (matchingShapes(match, candidateEdge)) {
										MinutiaPair pair = allocate();
										pair.probe = match.reference;
										pair.candidate = candidateReference;
										roots[totalRoots] = pair;
										++totalRoots;
										if (totalRoots >= Parameters.maxTriedRoots)
											return totalRoots;
									}
								}
							}
							++totalLookups;
							if (totalLookups >= Parameters.maxRootEdgeLookups)
								return totalRoots;
						}
					}
				}
			}
		}
		return totalRoots;
	}
	private int hashShape(EdgeShape edge) {
		int lengthBin = edge.length / Parameters.maxDistanceError;
		int referenceAngleBin = (int)(edge.referenceAngle / Parameters.maxAngleError);
		int neighborAngleBin = (int)(edge.neighborAngle / Parameters.maxAngleError);
		return (referenceAngleBin << 24) + (neighborAngleBin << 16) + lengthBin;
	}
	private boolean matchingShapes(EdgeShape probe, EdgeShape candidate) {
		int lengthDelta = probe.length - candidate.length;
		if (lengthDelta >= -Parameters.maxDistanceError && lengthDelta <= Parameters.maxDistanceError) {
			double complementaryAngleError = Angle.complementary(Parameters.maxAngleError);
			double referenceDelta = Angle.difference(probe.referenceAngle, candidate.referenceAngle);
			if (referenceDelta <= Parameters.maxAngleError || referenceDelta >= complementaryAngleError) {
				double neighborDelta = Angle.difference(probe.neighborAngle, candidate.neighborAngle);
				if (neighborDelta <= Parameters.maxAngleError || neighborDelta >= complementaryAngleError)
					return true;
			}
		}
		return false;
	}
	private double tryRoot(MinutiaPair root) {
		queue.add(root);
		do {
			addPair(queue.remove());
			collectEdges();
			skipPaired();
		} while (!queue.isEmpty());
		return computeScore();
	}
	private void clearPairing() {
		for (int i = 0; i < count; ++i) {
			byProbe[tree[i].probe] = null;
			byCandidate[tree[i].candidate] = null;
			release(tree[i]);
			tree[i] = null;
		}
		count = 0;
	}
	private void collectEdges() {
		MinutiaPair reference = tree[count - 1];
		NeighborEdge[] probeNeighbors = probeEdges[reference.probe];
		NeighborEdge[] candidateNeigbors = candidateEdges[reference.candidate];
		for (MinutiaPair pair : matchPairs(probeNeighbors, candidateNeigbors)) {
			pair.probeRef = reference.probe;
			pair.candidateRef = reference.candidate;
			if (byCandidate[pair.candidate] == null && byProbe[pair.probe] == null)
				queue.add(pair);
			else {
				if (byProbe[pair.probe] != null && byProbe[pair.probe].candidate == pair.candidate)
					addSupportingEdge(pair);
				release(pair);
			}
		}
	}
	private List<MinutiaPair> matchPairs(NeighborEdge[] probeStar, NeighborEdge[] candidateStar) {
		double complementaryAngleError = Angle.complementary(Parameters.maxAngleError);
		List<MinutiaPair> results = new ArrayList<>();
		int start = 0;
		int end = 0;
		for (int candidateIndex = 0; candidateIndex < candidateStar.length; ++candidateIndex) {
			NeighborEdge candidateEdge = candidateStar[candidateIndex];
			while (start < probeStar.length && probeStar[start].length < candidateEdge.length - Parameters.maxDistanceError)
				++start;
			if (end < start)
				end = start;
			while (end < probeStar.length && probeStar[end].length <= candidateEdge.length + Parameters.maxDistanceError)
				++end;
			for (int probeIndex = start; probeIndex < end; ++probeIndex) {
				NeighborEdge probeEdge = probeStar[probeIndex];
				double referenceDiff = Angle.difference(probeEdge.referenceAngle, candidateEdge.referenceAngle);
				if (referenceDiff <= Parameters.maxAngleError || referenceDiff >= complementaryAngleError) {
					double neighborDiff = Angle.difference(probeEdge.neighborAngle, candidateEdge.neighborAngle);
					if (neighborDiff <= Parameters.maxAngleError || neighborDiff >= complementaryAngleError) {
						MinutiaPair pair = allocate();
						pair.probe = probeEdge.neighbor;
						pair.candidate = candidateEdge.neighbor;
						pair.distance = candidateEdge.length;
						results.add(pair);
					}
				}
			}
		}
		return results;
	}
	private void skipPaired() {
		while (!queue.isEmpty() && (byProbe[queue.peek().probe] != null || byCandidate[queue.peek().candidate] != null)) {
			MinutiaPair pair = queue.remove();
			if (byProbe[pair.probe] != null && byProbe[pair.probe].candidate == pair.candidate)
				addSupportingEdge(pair);
			release(pair);
		}
	}
	private void addPair(MinutiaPair pair) {
		tree[count] = pair;
		byProbe[pair.probe] = pair;
		byCandidate[pair.candidate] = pair;
		++count;
	}
	private void addSupportingEdge(MinutiaPair pair) {
		++byProbe[pair.probe].supportingEdges;
		++byProbe[pair.probeRef].supportingEdges;
		logger.logSupportingEdge(pair);
	}
	private double computeScore() {
		double minutiaScore = Parameters.pairCountScore * count;
		double ratioScore = Parameters.pairFractionScore * (count / (double)probeMinutiae.length + count / (double)candidateMinutiae.length) / 2;
		double supportedScore = 0;
		double edgeScore = 0;
		double typeScore = 0;
		for (int i = 0; i < count; ++i) {
			MinutiaPair pair = tree[i];
			if (pair.supportingEdges >= Parameters.minSupportingEdges)
				supportedScore += Parameters.supportedCountScore;
			edgeScore += Parameters.edgeCountScore * (pair.supportingEdges + 1);
			if (probeMinutiae[pair.probe].type == candidateMinutiae[pair.candidate].type)
				typeScore += Parameters.correctTypeScore;
		}
		int innerDistanceRadius = (int)Math.round(Parameters.distanceErrorFlatness * Parameters.maxDistanceError);
		int innerAngleRadius = (int)Math.round(Parameters.angleErrorFlatness * Parameters.maxAngleError);
		int distanceErrorSum = 0;
		int angleErrorSum = 0;
		for (int i = 1; i < count; ++i) {
			MinutiaPair pair = tree[i];
			EdgeShape probeEdge = new EdgeShape(probeMinutiae[pair.probeRef], probeMinutiae[pair.probe]);
			EdgeShape candidateEdge = new EdgeShape(candidateMinutiae[pair.candidateRef], candidateMinutiae[pair.candidate]);
			distanceErrorSum += Math.max(innerDistanceRadius, Math.abs(probeEdge.length - candidateEdge.length));
			angleErrorSum += Math.max(innerAngleRadius, Angle.distance(probeEdge.referenceAngle, candidateEdge.referenceAngle));
			angleErrorSum += Math.max(innerAngleRadius, Angle.distance(probeEdge.neighborAngle, candidateEdge.neighborAngle));
		}
		double distanceScore = 0;
		double angleScore = 0;
		if (count >= 2) {
			double pairedDistanceError = Parameters.maxDistanceError * (count - 1);
			distanceScore = Parameters.distanceAccuracyScore * (pairedDistanceError - distanceErrorSum) / pairedDistanceError;
			double pairedAngleError = Parameters.maxAngleError * (count - 1) * 2;
			angleScore = Parameters.angleAccuracyScore * (pairedAngleError - angleErrorSum) / pairedAngleError;
		}
		double total = minutiaScore + ratioScore + supportedScore + edgeScore + typeScore + distanceScore + angleScore;
		double shaped = shape(total);
		logger.logScore(minutiaScore, ratioScore, supportedScore, edgeScore, typeScore, distanceScore, angleScore, total, shaped);
		return shaped;
	}
	private static double shape(double raw) {
		if (raw < Parameters.thresholdMaxFMR)
			return 0;
		if (raw < Parameters.thresholdFMR2)
			return interpolate(raw, Parameters.thresholdMaxFMR, Parameters.thresholdFMR2, 0, 3);
		if (raw < Parameters.thresholdFMR10)
			return interpolate(raw, Parameters.thresholdFMR2, Parameters.thresholdFMR10, 3, 7);
		if (raw < Parameters.thresholdFMR100)
			return interpolate(raw, Parameters.thresholdFMR10, Parameters.thresholdFMR100, 10, 10);
		if (raw < Parameters.thresholdFMR1000)
			return interpolate(raw, Parameters.thresholdFMR100, Parameters.thresholdFMR1000, 20, 10);
		if (raw < Parameters.thresholdFMR10_000)
			return interpolate(raw, Parameters.thresholdFMR1000, Parameters.thresholdFMR10_000, 30, 10);
		if (raw < Parameters.thresholdFMR100_000)
			return interpolate(raw, Parameters.thresholdFMR10_000, Parameters.thresholdFMR100_000, 40, 10);
		return (raw - Parameters.thresholdFMR100_000) / (Parameters.thresholdFMR100_000 - Parameters.thresholdFMR100) * 30 + 50;
	}
	private static double interpolate(double raw, double min, double max, double start, double length) {
		return (raw - min) / (max - min) * length + start;
	}
	private MinutiaPair allocate() {
		if (pooled > 0) {
			--pooled;
			MinutiaPair pair = pool[pooled];
			pool[pooled] = null;
			return pair;
		} else
			return new MinutiaPair();
	}
	private void release(MinutiaPair pair) {
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
