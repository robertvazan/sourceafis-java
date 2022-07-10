// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.transparency;

import java.util.*;
import com.machinezoo.sourceafis.engine.features.*;

@SuppressWarnings("unused")
public class ConsistentHashEntry {
	public final int key;
	public final List<IndexedEdge> edges;
	public ConsistentHashEntry(int key, List<IndexedEdge> edges) {
		this.key = key;
		this.edges = edges;
	}
}
