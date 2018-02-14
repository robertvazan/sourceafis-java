package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.util.*;

class JsonMinutia {
	int x;
	int y;
	double direction;
	String type;
	JsonMinutia(Minutia minutia) {
		x = minutia.position.x;
		y = minutia.position.y;
		direction = minutia.direction;
		type = minutia.type.json;
	}
	static List<JsonMinutia> map(Minutia[] minutiae) {
		return Arrays.stream(minutiae).map(JsonMinutia::new).collect(toList());
	}
}