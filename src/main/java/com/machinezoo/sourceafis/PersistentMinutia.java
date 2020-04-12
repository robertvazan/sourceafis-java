// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.util.*;

class PersistentMinutia {
	int x;
	int y;
	double direction;
	String type;
	PersistentMinutia(MutableMinutia mutable) {
		x = mutable.position.x;
		y = mutable.position.y;
		direction = mutable.direction;
		type = mutable.type.json;
	}
	MutableMinutia mutable() {
		MutableMinutia mutable = new MutableMinutia();
		mutable.position = new IntPoint(x, y);
		mutable.direction = direction;
		mutable.type = MinutiaType.BIFURCATION.json.equals(type) ? MinutiaType.BIFURCATION : MinutiaType.ENDING;
		return mutable;
	}
	void validate() {
		if (Math.abs(x) > 10_000 || Math.abs(y) > 10_000)
			throw new IllegalArgumentException("Minutia position out of range.");
		if (!DoubleAngle.normalized(direction))
			throw new IllegalArgumentException("Denormalized minutia direction.");
		Objects.requireNonNull(type, "Null minutia type.");
		if (!type.equals(MinutiaType.ENDING.json) && !type.equals(MinutiaType.BIFURCATION.json))
			throw new IllegalArgumentException("Unknown minutia type.");
	}
}
