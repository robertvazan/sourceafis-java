// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.extractor.skeletons;

import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.transparency.*;

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
