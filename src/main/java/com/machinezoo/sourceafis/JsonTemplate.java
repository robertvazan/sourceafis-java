package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.util.*;

class JsonTemplate {
	Cell size;
	List<JsonMinutia> minutiae;
	JsonTemplate(Cell size, Minutia[] minutiae) {
		this.size = size;
		this.minutiae = Arrays.stream(minutiae).map(JsonMinutia::new).collect(toList());
	}
	Minutia[] minutiae() {
		return minutiae.stream().map(Minutia::new).toArray(n -> new Minutia[n]);
	}
}