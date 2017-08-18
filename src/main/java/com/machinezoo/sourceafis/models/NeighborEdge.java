// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class NeighborEdge {
	public final EdgeShape shape;
	public final int neighbor;
	public NeighborEdge(EdgeShape shape, int neighbor) {
		this.shape = shape;
		this.neighbor = neighbor;
	}
}
