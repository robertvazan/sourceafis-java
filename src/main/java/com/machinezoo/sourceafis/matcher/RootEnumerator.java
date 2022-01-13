// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.matcher;

import java.util.*;
import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.features.*;

public class RootEnumerator {
	public static int enumerate(MatcherThread thread) {
		if (thread.roots.length < Parameters.MAX_TRIED_ROOTS)
			thread.roots = new MinutiaPair[Parameters.MAX_TRIED_ROOTS];
		int totalLookups = 0;
		int totalRoots = 0;
		int triedRoots = 0;
		thread.duplicates.clear();
		var candidate = thread.candidate.minutiae;
		for (boolean shortEdges : new boolean[] { false, true }) {
			for (int period = 1; period < candidate.length; ++period) {
				for (int phase = 0; phase <= period; ++phase) {
					for (int candidateReference = phase; candidateReference < candidate.length; candidateReference += period + 1) {
						int candidateNeighbor = (candidateReference + period) % candidate.length;
						EdgeShape candidateEdge = new EdgeShape(candidate[candidateReference], candidate[candidateNeighbor]);
						if ((candidateEdge.length >= Parameters.MIN_ROOT_EDGE_LENGTH) ^ shortEdges) {
							List<IndexedEdge> matches = thread.edgeHash.get(EdgeHash.hash(candidateEdge));
							if (matches != null) {
								for (IndexedEdge match : matches) {
									if (EdgeHash.matching(match, candidateEdge)) {
										int duplicateKey = (match.reference << 16) | candidateReference;
										if (thread.duplicates.add(duplicateKey)) {
											MinutiaPair pair = thread.allocate();
											pair.probe = match.reference;
											pair.candidate = candidateReference;
											thread.roots[totalRoots] = pair;
											++totalRoots;
										}
										++triedRoots;
										if (triedRoots >= Parameters.MAX_TRIED_ROOTS)
											return totalRoots;
									}
								}
							}
							++totalLookups;
							if (totalLookups >= Parameters.MAX_ROOT_EDGE_LOOKUPS)
								return totalRoots;
						}
					}
				}
			}
		}
		return totalRoots;
	}
}
