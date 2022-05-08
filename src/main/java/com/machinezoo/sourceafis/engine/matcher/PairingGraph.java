// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

import java.util.*;
import com.machinezoo.sourceafis.engine.templates.*;

public class PairingGraph {
	public final MinutiaPairPool pool;
	public int count;
	public MinutiaPair[] tree = new MinutiaPair[1];
	public MinutiaPair[] byProbe = new MinutiaPair[1];
	public MinutiaPair[] byCandidate = new MinutiaPair[1];
	public List<MinutiaPair> support = new ArrayList<>();
	public boolean supportEnabled;
	public PairingGraph(MinutiaPairPool pool) {
		this.pool = pool;
	}
	public void reserveProbe(ImmutableProbe probe) {
		int capacity = probe.template.minutiae.length;
		if (capacity > tree.length) {
			tree = new MinutiaPair[capacity];
			byProbe = new MinutiaPair[capacity];
		}
	}
	public void reserveCandidate(ImmutableTemplate candidate) {
		int capacity = candidate.minutiae.length;
		if (byCandidate.length < capacity)
			byCandidate = new MinutiaPair[capacity];
	}
	public void addPair(MinutiaPair pair) {
		tree[count] = pair;
		byProbe[pair.probe] = pair;
		byCandidate[pair.candidate] = pair;
		++count;
	}
	public void support(MinutiaPair pair) {
		if (byProbe[pair.probe] != null && byProbe[pair.probe].candidate == pair.candidate) {
			++byProbe[pair.probe].supportingEdges;
			++byProbe[pair.probeRef].supportingEdges;
			if (supportEnabled)
				support.add(pair);
			else
				pool.release(pair);
		} else
			pool.release(pair);
	}
	public void clear() {
		for (int i = 0; i < count; ++i) {
			byProbe[tree[i].probe] = null;
			byCandidate[tree[i].candidate] = null;
			/*
			 * Don't release root, just reset its supporting edge count.
			 */
			if (i > 0)
				pool.release(tree[i]);
			else
				tree[0].supportingEdges = 0;
			tree[i] = null;
		}
		count = 0;
		if (supportEnabled) {
			for (MinutiaPair pair : support)
				pool.release(pair);
			support.clear();
		}
	}
}
