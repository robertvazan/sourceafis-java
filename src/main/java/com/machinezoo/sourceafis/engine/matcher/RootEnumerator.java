// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.templates.*;

public class RootEnumerator {
	public static void enumerate(Probe probe, SearchTemplate candidate, RootList roots) {
		var cminutiae = candidate.minutiae;
		int lookups = 0;
		int tried = 0;
		for (boolean shortEdges : new boolean[] { false, true }) {
			for (int period = 1; period < cminutiae.length; ++period) {
				for (int phase = 0; phase <= period; ++phase) {
					for (int creference = phase; creference < cminutiae.length; creference += period + 1) {
						int cneighbor = (creference + period) % cminutiae.length;
						var cedge = new EdgeShape(cminutiae[creference], cminutiae[cneighbor]);
						if ((cedge.length >= Parameters.MIN_ROOT_EDGE_LENGTH) ^ shortEdges) {
							var matches = probe.hash.get(EdgeHashes.hash(cedge));
							if (matches != null) {
								for (var match : matches) {
									if (EdgeHashes.matching(match, cedge)) {
										int duplicateKey = (match.reference << 16) | creference;
										if (roots.duplicates.add(duplicateKey)) {
											MinutiaPair pair = roots.pool.allocate();
											pair.probe = match.reference;
											pair.candidate = creference;
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
