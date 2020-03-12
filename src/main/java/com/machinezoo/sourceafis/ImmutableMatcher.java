// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.util.*;
import it.unimi.dsi.fastutil.ints.*;

class ImmutableMatcher {
	static final ImmutableMatcher NULL = new ImmutableMatcher();
	final ImmutableTemplate template;
	final Int2ObjectMap<List<IndexedEdge>> edgeHash;
	private ImmutableMatcher() {
		template = ImmutableTemplate.EMPTY;
		edgeHash = new Int2ObjectOpenHashMap<>();
	}
	ImmutableMatcher(ImmutableTemplate template, Int2ObjectMap<List<IndexedEdge>> edgeHash) {
		this.template = template;
		this.edgeHash = edgeHash;
	}
}
