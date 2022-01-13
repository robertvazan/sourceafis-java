// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor.skeletons;

import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.transparency.*;

public class SkeletonTailFilter {
	public static void apply(Skeleton skeleton) {
		for (SkeletonMinutia minutia : skeleton.minutiae) {
			if (minutia.ridges.size() == 1 && minutia.ridges.get(0).end().ridges.size() >= 3)
				if (minutia.ridges.get(0).points.size() < Parameters.MIN_TAIL_LENGTH)
					minutia.ridges.get(0).detach();
		}
		SkeletonDotFilter.apply(skeleton);
		SkeletonKnotFilter.apply(skeleton);
		// https://sourceafis.machinezoo.com/transparency/removed-tails
		TransparencySink.current().logSkeleton("removed-tails", skeleton);
	}
}
