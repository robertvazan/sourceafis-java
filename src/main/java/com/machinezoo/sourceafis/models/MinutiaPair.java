// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class MinutiaPair {
	public int probe;
	public int candidate;
	public int probeRef;
	public int candidateRef;
	public int distance;
	public int supportingEdges;
	@Override public String toString() {
		return String.format("%d<->%d @ %d<->%d #%d", probe, candidate, probeRef, candidateRef, supportingEdges);
	}
}
