// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

import java.util.*;
import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.templates.*;

public class RootEnumerator {
	public static void enumerate(ImmutableProbe probe, ImmutableTemplate candidate, RootList roots) {
		var cminutiae = candidate.minutiae;
		int lookups = 0;
		int tried = 0;
		for (boolean shortEdges : new boolean[] { false, true }) {
			for (int period = 1; period < cminutiae.length; ++period) {
				for (int phase = 0; phase <= period; ++phase) {
					for (int candidateReference = phase; candidateReference < cminutiae.length; candidateReference += period + 1) {
						int candidateNeighbor = (candidateReference + period) % cminutiae.length;
						EdgeShape candidateEdge = new EdgeShape(cminutiae[candidateReference], cminutiae[candidateNeighbor]);
						if ((candidateEdge.length >= Parameters.MIN_ROOT_EDGE_LENGTH) ^ shortEdges) {
							List<IndexedEdge> matches = probe.hash.get(EdgeHash.hash(candidateEdge));
							if (matches != null) {
								for (IndexedEdge match : matches) {
									if (EdgeHash.matching(match, candidateEdge)) {
										int duplicateKey = (match.reference << 16) | candidateReference;
										if (roots.duplicates.add(duplicateKey)) {
											MinutiaPair pair = roots.pool.allocate();
											pair.probe = match.reference;
											pair.candidate = candidateReference;
											roots.add(pair);
										}
										++tried;
										if (tried >= Parameters.MAX_TRIED_ROOTS)
											return;
									}
								}
							}
							++lookups;
							if (lookups >= Parameters.MAX_ROOT_EDGE_LOOKUPS)
								return;
						}
					}
				}
			}
		}
	}
}
