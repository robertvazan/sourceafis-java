// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.features;

public class IndexedEdge extends EdgeShape {
	private final byte reference;
	private final byte neighbor;
	public IndexedEdge(FeatureMinutia[] minutiae, int reference, int neighbor) {
		super(minutiae[reference], minutiae[neighbor]);
		this.reference = (byte)reference;
		this.neighbor = (byte)neighbor;
	}
	public int reference() {
		return Byte.toUnsignedInt(reference);
	}
	public int neighbor() {
		return Byte.toUnsignedInt(neighbor);
	}
}
