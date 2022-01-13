// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor.skeletons;

import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.transparency.*;

public class SkeletonFragmentFilter {
	public static void apply(Skeleton skeleton) {
		for (SkeletonMinutia minutia : skeleton.minutiae)
			if (minutia.ridges.size() == 1) {
				SkeletonRidge ridge = minutia.ridges.get(0);
				if (ridge.end().ridges.size() == 1 && ridge.points.size() < Parameters.MIN_FRAGMENT_LENGTH)
					ridge.detach();
			}
		SkeletonDotFilter.apply(skeleton);
		// https://sourceafis.machinezoo.com/transparency/removed-fragments
		TransparencySink.current().logSkeleton("removed-fragments", skeleton);
	}
}
