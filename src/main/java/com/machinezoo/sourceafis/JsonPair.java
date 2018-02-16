// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.util.*;

class JsonPair {
	int probe;
	int candidate;
	JsonPair(int probe, int candidate) {
		this.probe = probe;
		this.candidate = candidate;
	}
	static List<JsonPair> roots(int count, MinutiaPair[] roots) {
		return Arrays.stream(roots).limit(count).map(p -> new JsonPair(p.probe, p.candidate)).collect(toList());
	}
}
