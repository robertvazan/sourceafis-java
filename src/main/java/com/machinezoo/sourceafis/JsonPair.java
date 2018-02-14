package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.util.*;
import lombok.*;

@AllArgsConstructor class JsonPair {
	int probe;
	int candidate;
	static List<JsonPair> roots(int count, MinutiaPair[] roots) {
		return Arrays.stream(roots).limit(count).map(p -> new JsonPair(p.probe, p.candidate)).collect(toList());
	}
}