// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.features;

import com.machinezoo.sourceafis.engine.primitives.*;

public class SearchMinutia {
	public final short x;
	public final short y;
	public final float direction;
	public final MinutiaType type;
	public SearchMinutia(FeatureMinutia feature) {
		this.x = (short)feature.position.x;
		this.y = (short)feature.position.y;
		this.direction = feature.direction;
		this.type = feature.type;
	}
	public FeatureMinutia feature() {
		return new FeatureMinutia(new IntPoint(x, y), direction, type);
	}
}
