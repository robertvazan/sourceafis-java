// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.transparency;

import static java.util.stream.Collectors.*;
import java.util.*;
import com.machinezoo.sourceafis.matcher.*;

public class ConsistentPairingGraph {
	public ConsistentMinutiaPair root;
	public List<ConsistentEdgePair> tree;
	public List<ConsistentEdgePair> support;
	public ConsistentPairingGraph(int count, MinutiaPair[] pairs, List<MinutiaPair> support) {
		root = new ConsistentMinutiaPair(pairs[0].probe, pairs[0].candidate);
		tree = Arrays.stream(pairs).limit(count).skip(1).map(ConsistentEdgePair::new).collect(toList());
		this.support = support.stream().map(ConsistentEdgePair::new).collect(toList());
	}
}
