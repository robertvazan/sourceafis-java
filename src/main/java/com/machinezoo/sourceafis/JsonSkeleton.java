// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.util.*;

class JsonSkeleton {
	int width;
	int height;
	List<IntPoint> minutiae;
	List<JsonSkeletonRidge> ridges;
	JsonSkeleton(Skeleton skeleton) {
		width = skeleton.size.x;
		height = skeleton.size.y;
		Map<SkeletonMinutia, Integer> offsets = new HashMap<>();
		for (int i = 0; i < skeleton.minutiae.size(); ++i)
			offsets.put(skeleton.minutiae.get(i), i);
		this.minutiae = skeleton.minutiae.stream().map(m -> m.position).collect(toList());
		ridges = skeleton.minutiae.stream()
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
