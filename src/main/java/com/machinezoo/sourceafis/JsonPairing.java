// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.util.*;

class JsonPairing {
	JsonPair root;
	List<JsonEdge> tree;
	List<JsonEdge> support;
	JsonPairing(int count, MinutiaPair[] pairs, List<JsonEdge> supporting) {
		root = new JsonPair(pairs[0].probe, pairs[0].candidate);
		tree = Arrays.stream(pairs).limit(count).skip(1).map(JsonEdge::new).collect(toList());
		support = supporting;
	}
}
