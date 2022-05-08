// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

import java.util.*;

public class MatcherThread {
	private static final ThreadLocal<MatcherThread> threads = new ThreadLocal<MatcherThread>() {
		/*
		 * ThreadLocal has method withInitial() that is more convenient,
		 * but that method alone would force whole SourceAFIS to require Android API level 26 instead of 24.
		 */
		@Override
		protected MatcherThread initialValue() {
			return new MatcherThread();
		}
	};
	public static MatcherThread current() {
		return threads.get();
	}
	public static void kill() {
		threads.remove();
	}
	public final MinutiaPairPool pool = new MinutiaPairPool();
	public final RootList roots = new RootList(pool);
	public final PairingGraph pairing = new PairingGraph(pool);
	public final PriorityQueue<MinutiaPair> queue = new PriorityQueue<>(Comparator.comparing(p -> p.distance));
	public final ScoringData score = new ScoringData();
}
