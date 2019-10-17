// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.util.*;

class JsonTemplate {
	String version = "3.7";
	int width;
	int height;
	List<JsonMinutia> minutiae;
	JsonTemplate(IntPoint size, ImmutableMinutia[] minutiae) {
		width = size.x;
		height = size.y;
		this.minutiae = Arrays.stream(minutiae).map(JsonMinutia::new).collect(toList());
	}
	IntPoint size() {
		return new IntPoint(width, height);
	}
	ImmutableMinutia[] minutiae() {
		return minutiae.stream().map(ImmutableMinutia::new).toArray(n -> new ImmutableMinutia[n]);
	}
	void validate() {
		/*
		 * Width and height are informative only. Don't validate them.
		 */
		Objects.requireNonNull(minutiae, "Null minutia array.");
		for (JsonMinutia minutia : minutiae) {
			Objects.requireNonNull(minutia, "Null minutia.");
			minutia.validate();
		}
	}
}
