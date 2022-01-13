// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor.skeletons;

import java.util.*;
import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.primitives.*;
import com.machinezoo.sourceafis.transparency.*;

public class SkeletonGapFilter {
	private static void addGapRidge(BooleanMatrix shadow, SkeletonGap gap, IntPoint[] line) {
		SkeletonRidge ridge = new SkeletonRidge();
		for (IntPoint point : line)
			ridge.points.add(point);
		ridge.start(gap.end1);
		ridge.end(gap.end2);
		for (IntPoint point : line)
			shadow.set(point, true);
	}
	private static boolean isRidgeOverlapping(IntPoint[] line, BooleanMatrix shadow) {
		for (int i = Parameters.TOLERATED_GAP_OVERLAP; i < line.length - Parameters.TOLERATED_GAP_OVERLAP; ++i)
			if (shadow.get(line[i]))
				return true;
		return false;
	}
	private static IntPoint angleSampleForGapRemoval(SkeletonMinutia minutia) {
		SkeletonRidge ridge = minutia.ridges.get(0);
		if (Parameters.GAP_ANGLE_OFFSET < ridge.points.size())
			return ridge.points.get(Parameters.GAP_ANGLE_OFFSET);
		else
			return ridge.end().position;
	}
	private static boolean isWithinGapLimits(SkeletonMinutia end1, SkeletonMinutia end2) {
		int distanceSq = end1.position.minus(end2.position).lengthSq();
		if (distanceSq <= Integers.sq(Parameters.MAX_RUPTURE_SIZE))
			return true;
		if (distanceSq > Integers.sq(Parameters.MAX_GAP_SIZE))
			return false;
		double gapDirection = DoubleAngle.atan(end1.position, end2.position);
		double direction1 = DoubleAngle.atan(end1.position, angleSampleForGapRemoval(end1));
		if (DoubleAngle.distance(direction1, DoubleAngle.opposite(gapDirection)) > Parameters.MAX_GAP_ANGLE)
			return false;
		double direction2 = DoubleAngle.atan(end2.position, angleSampleForGapRemoval(end2));
		if (DoubleAngle.distance(direction2, gapDirection) > Parameters.MAX_GAP_ANGLE)
			return false;
		return true;
	}
	public static void apply(Skeleton skeleton) {
		PriorityQueue<SkeletonGap> queue = new PriorityQueue<>();
		for (SkeletonMinutia end1 : skeleton.minutiae)
			if (end1.ridges.size() == 1 && end1.ridges.get(0).points.size() >= Parameters.SHORTEST_JOINED_ENDING)
				for (SkeletonMinutia end2 : skeleton.minutiae)
					if (end2 != end1 && end2.ridges.size() == 1 && end1.ridges.get(0).end() != end2
						&& end2.ridges.get(0).points.size() >= Parameters.SHORTEST_JOINED_ENDING && isWithinGapLimits(end1, end2)) {
						SkeletonGap gap = new SkeletonGap();
						gap.distance = end1.position.minus(end2.position).lengthSq();
						gap.end1 = end1;
						gap.end2 = end2;
						queue.add(gap);
					}
		BooleanMatrix shadow = skeleton.shadow();
		while (!queue.isEmpty()) {
			SkeletonGap gap = queue.remove();
			if (gap.end1.ridges.size() == 1 && gap.end2.ridges.size() == 1) {
				IntPoint[] line = gap.end1.position.lineTo(gap.end2.position);
				if (!isRidgeOverlapping(line, shadow))
					addGapRidge(shadow, gap, line);
			}
		}
		SkeletonKnotFilter.apply(skeleton);
		// https://sourceafis.machinezoo.com/transparency/removed-gaps
		TransparencySink.current().logSkeleton("removed-gaps", skeleton);
	}
}
