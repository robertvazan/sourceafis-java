// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor.minutiae;

import java.util.*;
import com.machinezoo.sourceafis.features.*;

public class MinutiaCollector {
	public static void collect(List<MutableMinutia> minutiae, Skeleton skeleton, MinutiaType type) {
		for (SkeletonMinutia sminutia : skeleton.minutiae)
			if (sminutia.ridges.size() == 1)
				minutiae.add(new MutableMinutia(sminutia.position, sminutia.ridges.get(0).direction(), type));
	}
	public static List<MutableMinutia> collect(Skeleton ridges, Skeleton valleys) {
		var minutiae = new ArrayList<MutableMinutia>();
		collect(minutiae, ridges, MinutiaType.ENDING);
		collect(minutiae, valleys, MinutiaType.BIFURCATION);
		return minutiae;
	}
}
