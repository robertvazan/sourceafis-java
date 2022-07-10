// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.extractor.minutiae;

import static java.util.stream.Collectors.*;
import java.util.*;
import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.features.*;

public class TopMinutiaeFilter {
	public static List<FeatureMinutia> apply(List<FeatureMinutia> minutiae) {
		if (minutiae.size() <= Parameters.MAX_MINUTIAE)
			return minutiae;
		return minutiae.stream()
			.sorted(Comparator.<FeatureMinutia>comparingInt(
				minutia -> minutiae.stream()
					.mapToInt(neighbor -> minutia.position.minus(neighbor.position).lengthSq())
					.sorted()
					.skip(Parameters.SORT_BY_NEIGHBOR)
					.findFirst().orElse(Integer.MAX_VALUE))
				.reversed())
			.limit(Parameters.MAX_MINUTIAE)
			.collect(toList());
	}
}
