// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.util.*;

class PersistentTemplate {
	String version;
	int width;
	int height;
	List<PersistentMinutia> minutiae;
	PersistentTemplate() {
	}
	PersistentTemplate(MutableTemplate mutable) {
		version = FingerprintCompatibility.version();
		width = mutable.size.x;
		height = mutable.size.y;
		this.minutiae = mutable.minutiae.stream().map(PersistentMinutia::new).collect(toList());
	}
	MutableTemplate mutable() {
		MutableTemplate mutable = new MutableTemplate();
		mutable.size = new IntPoint(width, height);
		mutable.minutiae = minutiae.stream().map(PersistentMinutia::mutable).collect(toList());
		return mutable;
	}
	void validate() {
		/*
		 * Width and height are informative only. Don't validate them.
		 */
		Objects.requireNonNull(minutiae, "Null minutia array.");
		for (PersistentMinutia minutia : minutiae) {
			Objects.requireNonNull(minutia, "Null minutia.");
			minutia.validate();
		}
	}
}
