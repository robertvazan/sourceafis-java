// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

import java.util.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.templates.*;
import it.unimi.dsi.fastutil.ints.*;

public class Probe {
	public static final Probe NULL = new Probe();
	public final SearchTemplate template;
	public final Int2ObjectMap<List<IndexedEdge>> hash;
	private Probe() {
		template = SearchTemplate.EMPTY;
		hash = new Int2ObjectOpenHashMap<>();
	}
	public Probe(SearchTemplate template, Int2ObjectMap<List<IndexedEdge>> edgeHash) {
		this.template = template;
		this.hash = edgeHash;
	}
}
