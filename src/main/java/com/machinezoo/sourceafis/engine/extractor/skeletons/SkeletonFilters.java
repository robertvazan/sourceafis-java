// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.extractor.skeletons;

import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class SkeletonFilters {
	public static void apply(Skeleton skeleton) {
		SkeletonDotFilter.apply(skeleton);
		// https://sourceafis.machinezoo.com/transparency/removed-dots
		TransparencySink.current().logSkeleton("removed-dots", skeleton);
		SkeletonPoreFilter.apply(skeleton);
		SkeletonGapFilter.apply(skeleton);
		SkeletonTailFilter.apply(skeleton);
		SkeletonFragmentFilter.apply(skeleton);
	}
}
