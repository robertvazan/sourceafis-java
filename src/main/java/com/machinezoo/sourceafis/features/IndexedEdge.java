// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.features;

public class IndexedEdge extends EdgeShape {
	public final int reference;
	public final int neighbor;
	public IndexedEdge(ImmutableMinutia[] minutiae, int reference, int neighbor) {
		super(minutiae[reference], minutiae[neighbor]);
		this.reference = reference;
		this.neighbor = neighbor;
	}
}
