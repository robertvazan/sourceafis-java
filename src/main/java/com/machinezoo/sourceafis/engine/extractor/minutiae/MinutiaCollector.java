// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.extractor.minutiae;

import java.util.*;
import com.machinezoo.sourceafis.engine.features.*;

public class MinutiaCollector {
	public static void collect(List<FeatureMinutia> minutiae, Skeleton skeleton, MinutiaType type) {
		for (SkeletonMinutia sminutia : skeleton.minutiae)
			if (sminutia.ridges.size() == 1)
				minutiae.add(new FeatureMinutia(sminutia.position, sminutia.ridges.get(0).direction(), type));
	}
	public static List<FeatureMinutia> collect(Skeleton ridges, Skeleton valleys) {
		var minutiae = new ArrayList<FeatureMinutia>();
		collect(minutiae, ridges, MinutiaType.ENDING);
		collect(minutiae, valleys, MinutiaType.BIFURCATION);
		return minutiae;
	}
}
