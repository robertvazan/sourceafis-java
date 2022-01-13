// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.features;

import com.machinezoo.sourceafis.primitives.*;

public class ImmutableMinutia {
	public final IntPoint position;
	public final double direction;
	public final MinutiaType type;
	public ImmutableMinutia(MutableMinutia mutable) {
		position = mutable.position;
		direction = mutable.direction;
		type = mutable.type;
	}
	public MutableMinutia mutable() {
		return new MutableMinutia(position, direction, type);
	}
}
