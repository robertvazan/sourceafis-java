// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.features;

import java.util.*;
import com.machinezoo.sourceafis.primitives.*;

public class SkeletonMinutia {
	public final IntPoint position;
	public final List<SkeletonRidge> ridges = new ArrayList<>();
	public SkeletonMinutia(IntPoint position) {
		this.position = position;
	}
	public void attachStart(SkeletonRidge ridge) {
		if (!ridges.contains(ridge)) {
			ridges.add(ridge);
			ridge.start(this);
		}
	}
	public void detachStart(SkeletonRidge ridge) {
		if (ridges.contains(ridge)) {
			ridges.remove(ridge);
			if (ridge.start() == this)
				ridge.start(null);
		}
	}
	@Override
	public String toString() {
		return String.format("%s*%d", position.toString(), ridges.size());
	}
}
