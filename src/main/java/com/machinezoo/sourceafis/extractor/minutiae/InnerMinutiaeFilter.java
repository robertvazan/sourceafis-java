// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor.minutiae;

import java.util.*;
import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.primitives.*;

public class InnerMinutiaeFilter {
	public static void apply(List<MutableMinutia> minutiae, BooleanMatrix mask) {
		minutiae.removeIf(minutia -> {
			IntPoint arrow = DoubleAngle.toVector(minutia.direction).multiply(-Parameters.MASK_DISPLACEMENT).round();
			return !mask.get(minutia.position.plus(arrow), false);
		});
	}
}
