// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

import com.machinezoo.sourceafis.engine.configuration.*;
import it.unimi.dsi.fastutil.ints.*;

public class RootList {
	public final MinutiaPairPool pool;
	public int count;
	public MinutiaPair[] pairs = new MinutiaPair[Parameters.MAX_TRIED_ROOTS];
	public final IntSet duplicates = new IntOpenHashSet();
	public RootList(MinutiaPairPool pool) {
		this.pool = pool;
	}
	public void add(MinutiaPair pair) {
		pairs[count] = pair;
		++count;
	}
	public void discard() {
		for (int i = 0; i < count; ++i) {
			pool.release(pairs[i]);
			pairs[i] = null;
		}
		count = 0;
		duplicates.clear();
	}
}
