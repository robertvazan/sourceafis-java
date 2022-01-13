// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.matcher;

import java.util.*;
import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.templates.*;
import it.unimi.dsi.fastutil.ints.*;

public class ImmutableMatcher {
	public static final ImmutableMatcher NULL = new ImmutableMatcher();
	public final ImmutableTemplate template;
	public final Int2ObjectMap<List<IndexedEdge>> edgeHash;
	private ImmutableMatcher() {
		template = ImmutableTemplate.EMPTY;
		edgeHash = new Int2ObjectOpenHashMap<>();
	}
	public ImmutableMatcher(ImmutableTemplate template, Int2ObjectMap<List<IndexedEdge>> edgeHash) {
		this.template = template;
		this.edgeHash = edgeHash;
	}
}
