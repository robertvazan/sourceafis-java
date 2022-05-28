// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

import java.util.*;
import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;

public class EdgeSpider {
	private static final double COMPLEMENTARY_MAX_ANGLE_ERROR = DoubleAngle.complementary(Parameters.MAX_ANGLE_ERROR);
	private static List<MinutiaPair> matchPairs(NeighborEdge[] pstar, NeighborEdge[] cstar, MinutiaPairPool pool) {
		List<MinutiaPair> results = new ArrayList<>();
		int start = 0;
		int end = 0;
		for (int cindex = 0; cindex < cstar.length; ++cindex) {
			var cedge = cstar[cindex];
			while (start < pstar.length && pstar[start].length < cedge.length - Parameters.MAX_DISTANCE_ERROR)
				++start;
			if (end < start)
				end = start;
			while (end < pstar.length && pstar[end].length <= cedge.length + Parameters.MAX_DISTANCE_ERROR)
				++end;
			for (int probeIndex = start; probeIndex < end; ++probeIndex) {
				NeighborEdge probeEdge = pstar[probeIndex];
				double referenceDiff = DoubleAngle.difference(probeEdge.referenceAngle, cedge.referenceAngle);
				if (referenceDiff <= Parameters.MAX_ANGLE_ERROR || referenceDiff >= COMPLEMENTARY_MAX_ANGLE_ERROR) {
					double neighborDiff = DoubleAngle.difference(probeEdge.neighborAngle, cedge.neighborAngle);
					if (neighborDiff <= Parameters.MAX_ANGLE_ERROR || neighborDiff >= COMPLEMENTARY_MAX_ANGLE_ERROR) {
						MinutiaPair pair = pool.allocate();
						pair.probe = probeEdge.neighbor;
						pair.candidate = cedge.neighbor;
						pair.distance = cedge.length;
						results.add(pair);
					}
				}
			}
		}
		return results;
	}
	private static void collectEdges(NeighborEdge[][] pedges, NeighborEdge[][] cedges, PairingGraph pairing, PriorityQueue<MinutiaPair> queue) {
		var reference = pairing.tree[pairing.count - 1];
		var pstar = pedges[reference.probe];
		var cstar = cedges[reference.candidate];
		for (var pair : matchPairs(pstar, cstar, pairing.pool)) {
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
