// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.extractor.minutiae;

import static java.util.stream.Collectors.*;
import java.util.*;
import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;

public class MinutiaCloudFilter {
	public static void apply(List<FeatureMinutia> minutiae) {
		int radiusSq = Integers.sq(Parameters.MINUTIA_CLOUD_RADIUS);
		minutiae.removeAll(minutiae.stream()
			.filter(minutia -> Parameters.MAX_CLOUD_SIZE < minutiae.stream()
				.filter(neighbor -> neighbor.position.minus(minutia.position).lengthSq() <= radiusSq)
				.count() - 1)
			.collect(toList()));
	}
}
