// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.features;

import com.machinezoo.sourceafis.engine.primitives.*;

public class FeatureMinutia {
	public final IntPoint position;
	public final float direction;
	public final MinutiaType type;
	public FeatureMinutia(IntPoint position, float direction, MinutiaType type) {
		this.position = position;
		this.direction = direction;
		this.type = type;
	}
}
