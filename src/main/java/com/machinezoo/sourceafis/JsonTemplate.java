// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.util.*;

class JsonTemplate {
	int width;
	int height;
	List<JsonMinutia> minutiae;
	JsonTemplate(Cell size, Minutia[] minutiae) {
		width = size.x;
		height = size.y;
		this.minutiae = Arrays.stream(minutiae).map(JsonMinutia::new).collect(toList());
	}
	Cell size() {
		return new Cell(width, height);
	}
	Minutia[] minutiae() {
		return minutiae.stream().map(Minutia::new).toArray(n -> new Minutia[n]);
	}
}
