// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.util.*;

class ImmutableTemplate {
	static final ImmutableTemplate EMPTY = new ImmutableTemplate();
	final IntPoint size;
	final ImmutableMinutia[] minutiae;
	final NeighborEdge[][] edges;
	private ImmutableTemplate() {
		size = new IntPoint(1, 1);
		minutiae = new ImmutableMinutia[0];
		edges = new NeighborEdge[0][];
	}
	private static final int PRIME = 1610612741;
	ImmutableTemplate(MutableTemplate mutable) {
		size = mutable.size;
		minutiae = mutable.minutiae.stream()
			.map(ImmutableMinutia::new)
			.sorted(Comparator
				.comparingInt((ImmutableMinutia m) -> ((m.position.x * PRIME) + m.position.y) * PRIME)
				.thenComparing(m -> m.position.x)
				.thenComparing(m -> m.position.y)
				.thenComparing(m -> m.direction)
				.thenComparing(m -> m.type))
			.toArray(ImmutableMinutia[]::new);
		// https://sourceafis.machinezoo.com/transparency/shuffled-minutiae
		FingerprintTransparency.current().log("shuffled-minutiae", this::mutable);
		edges = NeighborEdge.buildTable(minutiae);
	}
	MutableTemplate mutable() {
		MutableTemplate mutable = new MutableTemplate();
		mutable.size = size;
		mutable.minutiae = Arrays.stream(minutiae).map(ImmutableMinutia::mutable).collect(toList());
		return mutable;
	}
}
