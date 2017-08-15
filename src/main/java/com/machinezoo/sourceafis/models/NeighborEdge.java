// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class NeighborEdge {
	public final EdgeShape edge;
	public final int neighbor;
	public NeighborEdge(EdgeShape edge, int neighbor) {
		this.edge = edge;
		this.neighbor = neighbor;
	}
}
