// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class MinutiaPair {
	int probe;
	int candidate;
	int probeRef;
	int candidateRef;
	int distance;
	int supportingEdges;
	@Override public String toString() {
		return String.format("%d<->%d @ %d<->%d #%d", probe, candidate, probeRef, candidateRef, supportingEdges);
	}
}
