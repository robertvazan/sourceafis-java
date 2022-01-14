// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.extractor;

import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class AbsoluteContrastMask {
	public static BooleanMatrix compute(DoubleMatrix contrast) {
		BooleanMatrix result = new BooleanMatrix(contrast.size());
		for (IntPoint block : contrast.size())
			if (contrast.get(block) < Parameters.MIN_ABSOLUTE_CONTRAST)
				result.set(block, true);
		// https://sourceafis.machinezoo.com/transparency/absolute-contrast-mask
		TransparencySink.current().log("absolute-contrast-mask", result);
		return result;
	}
}
