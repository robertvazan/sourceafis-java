// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.util.*;

class JsonMinutia {
	int x;
	int y;
	double direction;
	String type;
	JsonMinutia(ImmutableMinutia minutia) {
		x = minutia.position.x;
		y = minutia.position.y;
		direction = minutia.direction;
		type = minutia.type.json;
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
