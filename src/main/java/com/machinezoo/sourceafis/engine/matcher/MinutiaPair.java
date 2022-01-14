// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

public class MinutiaPair {
	public int probe;
	public int candidate;
	public int probeRef;
	public int candidateRef;
	public int distance;
	public int supportingEdges;
	@Override
	public String toString() {
		return String.format("%d<->%d @ %d<->%d #%d", probe, candidate, probeRef, candidateRef, supportingEdges);
	}
}
