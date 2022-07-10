// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.templates;

import java.util.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class SearchTemplate {
	public static final SearchTemplate EMPTY = new SearchTemplate();
	public final IntPoint size;
	public final FeatureMinutia[] minutiae;
	public final NeighborEdge[][] edges;
	private SearchTemplate() {
		size = new IntPoint(1, 1);
		minutiae = new FeatureMinutia[0];
		edges = new NeighborEdge[0][];
	}
	private static final int PRIME = 1610612741;
	public SearchTemplate(FeatureTemplate features) {
		size = features.size;
		minutiae = features.minutiae.stream()
			.sorted(Comparator
				.comparingInt((FeatureMinutia m) -> ((m.position.x * PRIME) + m.position.y) * PRIME)
				.thenComparing(m -> m.position.x)
				.thenComparing(m -> m.position.y)
				.thenComparing(m -> m.direction)
				.thenComparing(m -> m.type))
			.toArray(FeatureMinutia[]::new);
		// https://sourceafis.machinezoo.com/transparency/shuffled-minutiae
		TransparencySink.current().log("shuffled-minutiae", this::features);
		edges = NeighborEdge.buildTable(minutiae);
	}
	public FeatureTemplate features() {
		return new FeatureTemplate(size, List.of(minutiae));
	}
}
