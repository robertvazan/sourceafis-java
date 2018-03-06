// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class ImmutableTemplate {
	final Cell size;
	final Minutia[] minutiae;
	final NeighborEdge[][] edges;
	ImmutableTemplate(TemplateBuilder builder) {
		size = builder.size;
		minutiae = builder.minutiae;
		edges = builder.edges;
	}
}
