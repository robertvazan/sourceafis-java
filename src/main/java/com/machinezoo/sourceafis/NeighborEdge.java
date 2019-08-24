// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class NeighborEdge extends EdgeShape {
	final int neighbor;
	NeighborEdge(ImmutableMinutia[] minutiae, int reference, int neighbor) {
		super(minutiae[reference], minutiae[neighbor]);
		this.neighbor = neighbor;
	}
}
