// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.transparency;

import static java.util.stream.Collectors.*;
import java.util.*;
import com.machinezoo.sourceafis.engine.matcher.*;

public class ConsistentMinutiaPair {
	public final int probe;
	public final int candidate;
	public ConsistentMinutiaPair(int probe, int candidate) {
		this.probe = probe;
		this.candidate = candidate;
	}
	public static List<ConsistentMinutiaPair> roots(int count, MinutiaPair[] roots) {
		return Arrays.stream(roots).limit(count).map(p -> new ConsistentMinutiaPair(p.probe, p.candidate)).collect(toList());
	}
}
