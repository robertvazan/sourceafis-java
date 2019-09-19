// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.util.*;
import gnu.trove.map.hash.*;
import gnu.trove.set.hash.*;

class MatchBuffer {
	private static final ThreadLocal<MatchBuffer> local = new ThreadLocal<MatchBuffer>() {
		/*
		 * ThreadLocal has method withInitial() that is more convenient,
		 * but that method alone would force whole SourceAFIS to require Android API level 26 instead of 24.
		 */
		@Override protected MatchBuffer initialValue() {
			return new MatchBuffer();
		}
	};
	FingerprintTransparency transparency = FingerprintTransparency.none;
	ImmutableTemplate probe;
	private TIntObjectHashMap<List<IndexedEdge>> edgeHash;
	ImmutableTemplate candidate;
	private MinutiaPair[] pool = new MinutiaPair[1];
	private int pooled;
	private PriorityQueue<MinutiaPair> queue = new PriorityQueue<>(Comparator.comparing(p -> p.distance));
	int count;
	MinutiaPair[] tree;
	private MinutiaPair[] byProbe;
	private MinutiaPair[] byCandidate;
	private MinutiaPair[] roots;
	private final TIntHashSet duplicates = new TIntHashSet();
	private Score score = new Score();
	static MatchBuffer current() {
		return local.get();
	}
	void selectMatcher(ImmutableMatcher matcher) {
		probe = matcher.template;
		if (tree == null || probe.minutiae.length > tree.length) {
			tree = new MinutiaPair[probe.minutiae.length];
			byProbe = new MinutiaPair[probe.minutiae.length];
		}
		edgeHash = matcher.edgeHash;
	}
	void selectCandidate(ImmutableTemplate template) {
		candidate = template;
		if (byCandidate == null || byCandidate.length < candidate.minutiae.length)
			byCandidate = new MinutiaPair[candidate.minutiae.length];
	}
	double match() {
		try {
			int totalRoots = enumerateRoots();
			// https://sourceafis.machinezoo.com/transparency/root-pairs
			transparency.logRootPairs(totalRoots, roots);
			double high = 0;
			int best = -1;
			for (int i = 0; i < totalRoots; ++i) {
				double partial = tryRoot(roots[i]);
				if (partial > high) {
					high = partial;
					best = i;
				}
				clearPairing();
			}
			// https://sourceafis.machinezoo.com/transparency/best-match
			transparency.logBestMatch(best);
			return high;
		} catch (Throwable e) {
			local.remove();
			throw e;
		}
	}
	private int enumerateRoots() {
		if (roots == null || roots.length < Parameters.maxTriedRoots)
			roots = new MinutiaPair[Parameters.maxTriedRoots];
		int totalLookups = 0;
		int totalRoots = 0;
		int triedRoots = 0;
		duplicates.clear();
		for (boolean shortEdges : new boolean[] { false, true }) {
			for (int period = 1; period < candidate.minutiae.length; ++period) {
				for (int phase = 0; phase <= period; ++phase) {
					for (int candidateReference = phase; candidateReference < candidate.minutiae.length; candidateReference += period + 1) {
						int candidateNeighbor = (candidateReference + period) % candidate.minutiae.length;
						EdgeShape candidateEdge = new EdgeShape(candidate.minutiae[candidateReference], candidate.minutiae[candidateNeighbor]);
						if ((candidateEdge.length >= Parameters.minRootEdgeLength) ^ shortEdges) {
							List<IndexedEdge> matches = edgeHash.get(hashShape(candidateEdge));
							if (matches != null) {
								for (IndexedEdge match : matches) {
									if (matchingShapes(match, candidateEdge)) {
										int duplicateKey = (match.reference << 16) | candidateReference;
										if (!duplicates.contains(duplicateKey)) {
											duplicates.add(duplicateKey);
											MinutiaPair pair = allocate();
											pair.probe = match.reference;
											pair.candidate = candidateReference;
											roots[totalRoots] = pair;
											++totalRoots;
										}
										++triedRoots;
										if (triedRoots >= Parameters.maxTriedRoots)
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
			double complementaryAngleError = DoubleAngle.complementary(Parameters.maxAngleError);
			double referenceDelta = DoubleAngle.difference(probe.referenceAngle, candidate.referenceAngle);
			if (referenceDelta <= Parameters.maxAngleError || referenceDelta >= complementaryAngleError) {
				double neighborDelta = DoubleAngle.difference(probe.neighborAngle, candidate.neighborAngle);
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
		// https://sourceafis.machinezoo.com/transparency/pairing
		transparency.logPairing(count, tree);
		score.compute(this);
		// https://sourceafis.machinezoo.com/transparency/score
		transparency.logScore(score);
		return score.shapedScore;
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
		NeighborEdge[] probeNeighbors = probe.edges[reference.probe];
		NeighborEdge[] candidateNeigbors = candidate.edges[reference.candidate];
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
		double complementaryAngleError = DoubleAngle.complementary(Parameters.maxAngleError);
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
				double referenceDiff = DoubleAngle.difference(probeEdge.referenceAngle, candidateEdge.referenceAngle);
				if (referenceDiff <= Parameters.maxAngleError || referenceDiff >= complementaryAngleError) {
					double neighborDiff = DoubleAngle.difference(probeEdge.neighborAngle, candidateEdge.neighborAngle);
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
		// https://sourceafis.machinezoo.com/transparency/pairing
		transparency.logSupportingEdge(pair);
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
