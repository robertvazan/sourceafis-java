// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.matcher;

import java.util.*;
import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.primitives.*;

public class EdgeSpider {
	private static void addPair(MatcherThread thread, MinutiaPair pair) {
		thread.tree[thread.count] = pair;
		thread.byProbe[pair.probe] = pair;
		thread.byCandidate[pair.candidate] = pair;
		++thread.count;
	}
	private static void support(MatcherThread thread, MinutiaPair pair) {
		if (thread.byProbe[pair.probe] != null && thread.byProbe[pair.probe].candidate == pair.candidate) {
			++thread.byProbe[pair.probe].supportingEdges;
			++thread.byProbe[pair.probeRef].supportingEdges;
			if (thread.reportSupport)
				thread.support.add(pair);
			else
				thread.release(pair);
		} else
			thread.release(pair);
	}
	private static List<MinutiaPair> matchPairs(MatcherThread thread, NeighborEdge[] probeStar, NeighborEdge[] candidateStar) {
		double complementaryAngleError = DoubleAngle.complementary(Parameters.MAX_ANGLE_ERROR);
		List<MinutiaPair> results = new ArrayList<>();
		int start = 0;
		int end = 0;
		for (int candidateIndex = 0; candidateIndex < candidateStar.length; ++candidateIndex) {
			NeighborEdge candidateEdge = candidateStar[candidateIndex];
			while (start < probeStar.length && probeStar[start].length < candidateEdge.length - Parameters.MAX_DISTANCE_ERROR)
				++start;
			if (end < start)
				end = start;
			while (end < probeStar.length && probeStar[end].length <= candidateEdge.length + Parameters.MAX_DISTANCE_ERROR)
				++end;
			for (int probeIndex = start; probeIndex < end; ++probeIndex) {
				NeighborEdge probeEdge = probeStar[probeIndex];
				double referenceDiff = DoubleAngle.difference(probeEdge.referenceAngle, candidateEdge.referenceAngle);
				if (referenceDiff <= Parameters.MAX_ANGLE_ERROR || referenceDiff >= complementaryAngleError) {
					double neighborDiff = DoubleAngle.difference(probeEdge.neighborAngle, candidateEdge.neighborAngle);
					if (neighborDiff <= Parameters.MAX_ANGLE_ERROR || neighborDiff >= complementaryAngleError) {
						MinutiaPair pair = thread.allocate();
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
	private static void collectEdges(MatcherThread thread) {
		MinutiaPair reference = thread.tree[thread.count - 1];
		NeighborEdge[] probeNeighbors = thread.probe.edges[reference.probe];
		NeighborEdge[] candidateNeigbors = thread.candidate.edges[reference.candidate];
		for (MinutiaPair pair : matchPairs(thread, probeNeighbors, candidateNeigbors)) {
			pair.probeRef = reference.probe;
			pair.candidateRef = reference.candidate;
			if (thread.byCandidate[pair.candidate] == null && thread.byProbe[pair.probe] == null)
				thread.queue.add(pair);
			else
				support(thread, pair);
		}
	}
	private static void skipPaired(MatcherThread thread) {
		while (!thread.queue.isEmpty() && (thread.byProbe[thread.queue.peek().probe] != null || thread.byCandidate[thread.queue.peek().candidate] != null))
			support(thread, thread.queue.remove());
	}
	public static double tryRoot(MatcherThread thread, MinutiaPair root) {
		thread.queue.add(root);
		do {
			addPair(thread, thread.queue.remove());
			collectEdges(thread);
			skipPaired(thread);
		} while (!thread.queue.isEmpty());
		// https://sourceafis.machinezoo.com/transparency/pairing
		thread.transparency.logPairing(thread.count, thread.tree, thread.support);
		thread.score.compute(thread);
		// https://sourceafis.machinezoo.com/transparency/score
		thread.transparency.logScore(thread.score);
		return thread.score.shapedScore;
	}
	public static void clearPairing(MatcherThread thread) {
		for (int i = 0; i < thread.count; ++i) {
			thread.byProbe[thread.tree[i].probe] = null;
			thread.byCandidate[thread.tree[i].candidate] = null;
			thread.release(thread.tree[i]);
			thread.tree[i] = null;
		}
		thread.count = 0;
		if (thread.reportSupport) {
			for (MinutiaPair pair : thread.support)
				thread.release(pair);
			thread.support.clear();
		}
	}
}
