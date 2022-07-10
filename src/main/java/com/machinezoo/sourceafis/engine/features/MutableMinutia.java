// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.features;

import com.machinezoo.sourceafis.engine.primitives.*;

public class MutableMinutia {
	public IntPoint position;
	public double direction;
	public MinutiaType type;
	public MutableMinutia() {
	}
	public MutableMinutia(IntPoint position, double direction, MinutiaType type) {
		this.position = position;
		this.direction = direction;
		this.type = type;
	}
}
