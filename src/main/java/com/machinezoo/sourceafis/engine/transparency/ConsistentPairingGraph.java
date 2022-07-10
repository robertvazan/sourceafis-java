// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.transparency;

import static java.util.stream.Collectors.*;
import java.util.*;
import com.machinezoo.sourceafis.engine.matcher.*;

public class ConsistentPairingGraph {
	public final ConsistentMinutiaPair root;
	public final List<ConsistentEdgePair> tree;
	public final List<ConsistentEdgePair> support;
	public ConsistentPairingGraph(PairingGraph pairing) {
		root = new ConsistentMinutiaPair(pairing.tree[0].probe, pairing.tree[0].candidate);
		tree = Arrays.stream(pairing.tree).limit(pairing.count).skip(1).map(ConsistentEdgePair::new).collect(toList());
		this.support = pairing.support.stream().map(ConsistentEdgePair::new).collect(toList());
	}
}
