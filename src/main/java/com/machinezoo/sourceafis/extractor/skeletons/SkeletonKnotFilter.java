// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor.skeletons;

import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.primitives.*;

public class SkeletonKnotFilter {
	public static void apply(Skeleton skeleton) {
		for (SkeletonMinutia minutia : skeleton.minutiae) {
			if (minutia.ridges.size() == 2 && minutia.ridges.get(0).reversed != minutia.ridges.get(1)) {
				SkeletonRidge extended = minutia.ridges.get(0).reversed;
				SkeletonRidge removed = minutia.ridges.get(1);
				if (extended.points.size() < removed.points.size()) {
					SkeletonRidge tmp = extended;
					extended = removed;
					removed = tmp;
					extended = extended.reversed;
					removed = removed.reversed;
				}
				extended.points.remove(extended.points.size() - 1);
				for (IntPoint point : removed.points)
					extended.points.add(point);
				extended.end(removed.end());
				removed.detach();
			}
		}
		SkeletonDotFilter.apply(skeleton);
	}
}
