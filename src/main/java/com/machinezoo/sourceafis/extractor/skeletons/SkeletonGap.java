// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor.skeletons;

import com.machinezoo.sourceafis.features.*;

class SkeletonGap implements Comparable<SkeletonGap> {
	int distance;
	SkeletonMinutia end1;
	SkeletonMinutia end2;
	@Override
	public int compareTo(SkeletonGap other) {
		int distanceCmp = Integer.compare(distance, other.distance);
		if (distanceCmp != 0)
			return distanceCmp;
		int end1Cmp = end1.position.compareTo(other.end1.position);
		if (end1Cmp != 0)
			return end1Cmp;
		return end2.position.compareTo(other.end2.position);
	}
}
