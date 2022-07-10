// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.templates;

import java.util.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;

public class FeatureTemplate {
	public final IntPoint size;
	public final List<FeatureMinutia> minutiae;
	public FeatureTemplate(IntPoint size, List<FeatureMinutia> minutiae) {
		this.size = size;
		this.minutiae = minutiae;
	}
}
