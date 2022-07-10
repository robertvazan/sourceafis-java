// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.transparency;

import static java.util.stream.Collectors.*;
import java.util.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;

public class ConsistentSkeleton {
	public final int width;
	public final int height;
	public final List<IntPoint> minutiae;
	public final List<ConsistentSkeletonRidge> ridges;
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
				.map(r -> new ConsistentSkeletonRidge(offsets.get(r.start()), offsets.get(r.end()), r.points)))
			.collect(toList());
	}
}
