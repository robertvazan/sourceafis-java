// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

import java.util.*;
import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;

public class EdgeSpider {
	private static List<MinutiaPair> matchPairs(NeighborEdge[] pstar, NeighborEdge[] cstar, MinutiaPairPool pool) {
		double complementaryAngleError = DoubleAngle.complementary(Parameters.MAX_ANGLE_ERROR);
		List<MinutiaPair> results = new ArrayList<>();
		int start = 0;
		int end = 0;
		for (int candidateIndex = 0; candidateIndex < cstar.length; ++candidateIndex) {
			NeighborEdge candidateEdge = cstar[candidateIndex];
			while (start < pstar.length && pstar[start].length < candidateEdge.length - Parameters.MAX_DISTANCE_ERROR)
				++start;
			if (end < start)
				end = start;
			while (end < pstar.length && pstar[end].length <= candidateEdge.length + Parameters.MAX_DISTANCE_ERROR)
				++end;
			for (int probeIndex = start; probeIndex < end; ++probeIndex) {
				NeighborEdge probeEdge = pstar[probeIndex];
				double referenceDiff = DoubleAngle.difference(probeEdge.referenceAngle, candidateEdge.referenceAngle);
				if (referenceDiff <= Parameters.MAX_ANGLE_ERROR || referenceDiff >= complementaryAngleError) {
					double neighborDiff = DoubleAngle.difference(probeEdge.neighborAngle, candidateEdge.neighborAngle);
					if (neighborDiff <= Parameters.MAX_ANGLE_ERROR || neighborDiff >= complementaryAngleError) {
						MinutiaPair pair = pool.allocate();
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
	private static void collectEdges(NeighborEdge[][] pedges, NeighborEdge[][] cedges, PairingGraph pairing, PriorityQueue<MinutiaPair> queue) {
		MinutiaPair reference = pairing.tree[pairing.count - 1];
		NeighborEdge[] pstar = pedges[reference.probe];
		NeighborEdge[] cstar = cedges[reference.candidate];
		for (MinutiaPair pair : matchPairs(pstar, cstar, pairing.pool)) {
			pair.probeRef = reference.probe;
			pair.candidateRef = reference.candidate;
			if (pairing.byCandidate[pair.candidate] == null && pairing.byProbe[pair.probe] == null)
				queue.add(pair);
			else
				pairing.support(pair);
		}
	}
	private static void skipPaired(PairingGraph pairing, PriorityQueue<MinutiaPair> queue) {
		while (!queue.isEmpty() && (pairing.byProbe[queue.peek().probe] != null || pairing.byCandidate[queue.peek().candidate] != null))
			pairing.support(queue.remove());
	}
	public static void crawl(NeighborEdge[][] pedges, NeighborEdge[][] cedges, PairingGraph pairing, MinutiaPair root, PriorityQueue<MinutiaPair> queue) {
		queue.add(root);
		do {
			pairing.addPair(queue.remove());
			collectEdges(pedges, cedges, pairing, queue);
			skipPaired(pairing, queue);
		} while (!queue.isEmpty());
	}
}
