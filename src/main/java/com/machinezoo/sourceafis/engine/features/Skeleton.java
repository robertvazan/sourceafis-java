// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.features;

import java.util.*;
import com.machinezoo.sourceafis.engine.primitives.*;

public class Skeleton {
	public final SkeletonType type;
	public final IntPoint size;
	public final List<SkeletonMinutia> minutiae = new ArrayList<>();
	public Skeleton(SkeletonType type, IntPoint size) {
		this.type = type;
		this.size = size;
	}
	public void addMinutia(SkeletonMinutia minutia) {
		minutiae.add(minutia);
	}
	public void removeMinutia(SkeletonMinutia minutia) {
		minutiae.remove(minutia);
	}
	public BooleanMatrix shadow() {
		BooleanMatrix shadow = new BooleanMatrix(size);
		for (SkeletonMinutia minutia : minutiae) {
			shadow.set(minutia.position, true);
			for (SkeletonRidge ridge : minutia.ridges)
				if (ridge.start().position.y <= ridge.end().position.y)
					for (IntPoint point : ridge.points)
						shadow.set(point, true);
		}
		return shadow;
	}
}
