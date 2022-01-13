// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor.skeletons;

import java.util.*;
import com.machinezoo.sourceafis.features.*;

public class SkeletonDotFilter {
	public static void apply(Skeleton skeleton) {
		List<SkeletonMinutia> removed = new ArrayList<>();
		for (SkeletonMinutia minutia : skeleton.minutiae)
			if (minutia.ridges.isEmpty())
				removed.add(minutia);
		for (SkeletonMinutia minutia : removed)
			skeleton.removeMinutia(minutia);
	}
}
