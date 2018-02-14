package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.util.*;

class JsonSkeleton {
	List<Cell> minutiae;
	List<JsonSkeletonRidge> ridges;
	JsonSkeleton(List<SkeletonMinutia> minutiae) {
		Map<SkeletonMinutia, Integer> offsets = new HashMap<>();
		for (int i = 0; i < minutiae.size(); ++i)
			offsets.put(minutiae.get(i), i);
		this.minutiae = minutiae.stream().map(m -> m.position).collect(toList());
		ridges = minutiae.stream()
			.flatMap(m -> m.ridges.stream()
				.filter(r -> r.points instanceof CircularList)
				.map(r -> {
					JsonSkeletonRidge jr = new JsonSkeletonRidge();
					jr.start = offsets.get(r.start());
					jr.end = offsets.get(r.end());
					jr.length = r.points.size();
					return jr;
				}))
			.collect(toList());
	}
}