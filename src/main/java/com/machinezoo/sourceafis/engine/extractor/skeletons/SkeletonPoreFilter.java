// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.extractor.skeletons;

import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class SkeletonPoreFilter {
	public static void apply(Skeleton skeleton) {
		for (SkeletonMinutia minutia : skeleton.minutiae) {
			if (minutia.ridges.size() == 3) {
				for (int exit = 0; exit < 3; ++exit) {
					SkeletonRidge exitRidge = minutia.ridges.get(exit);
					SkeletonRidge arm1 = minutia.ridges.get((exit + 1) % 3);
					SkeletonRidge arm2 = minutia.ridges.get((exit + 2) % 3);
					if (arm1.end() == arm2.end() && exitRidge.end() != arm1.end() && arm1.end() != minutia && exitRidge.end() != minutia) {
						SkeletonMinutia end = arm1.end();
						if (end.ridges.size() == 3 && arm1.points.size() <= Parameters.MAX_PORE_ARM && arm2.points.size() <= Parameters.MAX_PORE_ARM) {
							arm1.detach();
							arm2.detach();
							SkeletonRidge merged = new SkeletonRidge();
							merged.start(minutia);
							merged.end(end);
							for (IntPoint point : minutia.position.lineTo(end.position))
								merged.points.add(point);
						}
						break;
					}
				}
			}
		}
		SkeletonKnotFilter.apply(skeleton);
		// https://sourceafis.machinezoo.com/transparency/removed-pores
		TransparencySink.current().logSkeleton("removed-pores", skeleton);
	}
}
