// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.matcher;

import java.util.*;
import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.templates.*;
import it.unimi.dsi.fastutil.ints.*;

public class RootEnumerator {
	private final MinutiaPairPool pool;
	public int count;
	public MinutiaPair[] pairs = new MinutiaPair[Parameters.MAX_TRIED_ROOTS];
	private final IntSet duplicates = new IntOpenHashSet();
	public RootEnumerator(MinutiaPairPool pool) {
		this.pool = pool;
	}
	public void enumerate(ImmutableProbe probe, ImmutableTemplate candidate) {
		var cminutiae = candidate.minutiae;
		count = 0;
		int lookups = 0;
		int tried = 0;
		duplicates.clear();
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
										if (duplicates.add(duplicateKey)) {
											MinutiaPair pair = pool.allocate();
											pair.probe = match.reference;
											pair.candidate = candidateReference;
											pairs[count] = pair;
											++count;
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
