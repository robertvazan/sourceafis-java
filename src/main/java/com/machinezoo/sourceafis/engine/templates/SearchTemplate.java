// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.templates;

import static java.util.stream.Collectors.*;
import java.util.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class SearchTemplate {
	public static final SearchTemplate EMPTY = new SearchTemplate();
	public final short width;
	public final short height;
	public final SearchMinutia[] minutiae;
	public final NeighborEdge[][] edges;
	private SearchTemplate() {
		width = 1;
		height = 1;
		minutiae = new SearchMinutia[0];
		edges = new NeighborEdge[0][];
	}
	private static final int PRIME = 1610612741;
	public SearchTemplate(FeatureTemplate features) {
		width = (short)features.size.x;
		height = (short)features.size.y;
		minutiae = features.minutiae.stream()
			.map(SearchMinutia::new)
			.sorted(Comparator
				.comparingInt((SearchMinutia m) -> ((m.x * PRIME) + m.y) * PRIME)
				.thenComparingInt(m -> m.x)
				.thenComparingInt(m -> m.y)
				.thenComparingDouble(m -> m.direction)
				.thenComparing(m -> m.type))
			.toArray(SearchMinutia[]::new);
		// https://sourceafis.machinezoo.com/transparency/shuffled-minutiae
		TransparencySink.current().log("shuffled-minutiae", this::features);
		edges = NeighborEdge.buildTable(minutiae);
	}
	public FeatureTemplate features() {
		return new FeatureTemplate(new IntPoint(width, height), Arrays.stream(minutiae).map(m -> m.feature()).collect(toList()));
	}
}
