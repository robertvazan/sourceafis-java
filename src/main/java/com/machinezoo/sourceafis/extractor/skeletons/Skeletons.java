// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor.skeletons;

import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.primitives.*;
import com.machinezoo.sourceafis.transparency.*;

public class Skeletons {
	public static Skeleton create(BooleanMatrix binary, SkeletonType type) {
		// https://sourceafis.machinezoo.com/transparency/binarized-skeleton
		TransparencySink.current().log(type.prefix + "binarized-skeleton", binary);
		var thinned = BinaryThinning.thin(binary, type);
		var skeleton = SkeletonTracing.trace(thinned, type);
		SkeletonFilters.apply(skeleton);
		return skeleton;
	}
}
