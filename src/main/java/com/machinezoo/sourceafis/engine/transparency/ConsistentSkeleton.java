// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.transparency;

import static java.util.stream.Collectors.*;
import java.util.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;

public class ConsistentSkeleton {
	public int width;
	public int height;
	public List<IntPoint> minutiae;
	public List<ConsistentSkeletonRidge> ridges;
	public ConsistentSkeleton(Skeleton skeleton) {
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
					ConsistentSkeletonRidge jr = new ConsistentSkeletonRidge();
					jr.start = offsets.get(r.start());
					jr.end = offsets.get(r.end());
					jr.points = r.points;
					return jr;
				}))
			.collect(toList());
	}
}
