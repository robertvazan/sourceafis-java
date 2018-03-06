// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class ImmutableTemplate {
	static final ImmutableTemplate empty = new ImmutableTemplate();
	final Cell size;
	final Minutia[] minutiae;
	final NeighborEdge[][] edges;
	private ImmutableTemplate() {
		size = new Cell(1, 1);
		minutiae = new Minutia[0];
		edges = new NeighborEdge[0][];
	}
	ImmutableTemplate(TemplateBuilder builder) {
		size = builder.size;
		minutiae = builder.minutiae;
		edges = builder.edges;
	}
}
