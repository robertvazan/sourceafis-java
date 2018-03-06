package com.machinezoo.sourceafis;

import java.util.*;
import gnu.trove.map.hash.*;

class ImmutableMatcher {
	final ImmutableTemplate template;
	final TIntObjectHashMap<List<IndexedEdge>> edgeHash;
	ImmutableMatcher(ImmutableTemplate template, TIntObjectHashMap<List<IndexedEdge>> edgeHash) {
		this.template = template;
		this.edgeHash = edgeHash;
	}
}
