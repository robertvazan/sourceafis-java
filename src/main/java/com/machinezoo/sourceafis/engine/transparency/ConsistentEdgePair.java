// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.transparency;

import com.machinezoo.sourceafis.engine.matcher.*;

public class ConsistentEdgePair {
	public final int probeFrom;
	public final int probeTo;
	public final int candidateFrom;
	public final int candidateTo;
	public ConsistentEdgePair(MinutiaPair pair) {
		probeFrom = pair.probeRef;
		probeTo = pair.probe;
		candidateFrom = pair.candidateRef;
		candidateTo = pair.candidate;
	}
}
