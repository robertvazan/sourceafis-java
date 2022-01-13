// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.matcher;

import java.util.*;
import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.templates.*;
import it.unimi.dsi.fastutil.ints.*;

public class ImmutableProbe {
	public static final ImmutableProbe NULL = new ImmutableProbe();
	public final ImmutableTemplate template;
	public final Int2ObjectMap<List<IndexedEdge>> hash;
	private ImmutableProbe() {
		template = ImmutableTemplate.EMPTY;
		hash = new Int2ObjectOpenHashMap<>();
	}
	public ImmutableProbe(ImmutableTemplate template, Int2ObjectMap<List<IndexedEdge>> edgeHash) {
		this.template = template;
		this.hash = edgeHash;
	}
}
