// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor.minutiae;

import static java.util.stream.Collectors.*;
import java.util.*;
import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.features.*;

public class TopMinutiaeFilter {
	public static List<MutableMinutia> apply(List<MutableMinutia> minutiae) {
		if (minutiae.size() <= Parameters.MAX_MINUTIAE)
			return minutiae;
		return minutiae.stream()
			.sorted(Comparator.<MutableMinutia>comparingInt(
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
