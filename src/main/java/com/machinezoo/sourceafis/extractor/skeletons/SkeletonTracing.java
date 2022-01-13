// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor.skeletons;

import static java.util.stream.Collectors.*;
import java.util.*;
import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.primitives.*;
import com.machinezoo.sourceafis.transparency.*;

public class SkeletonTracing {
	private static List<IntPoint> findMinutiae(BooleanMatrix thinned) {
		List<IntPoint> result = new ArrayList<>();
		for (IntPoint at : thinned.size())
			if (thinned.get(at)) {
				int count = 0;
				for (IntPoint relative : IntPoint.CORNER_NEIGHBORS)
					if (thinned.get(at.plus(relative), false))
						++count;
				if (count == 1 || count > 2)
					result.add(at);
			}
		return result;
	}
	private static Map<IntPoint, List<IntPoint>> linkNeighboringMinutiae(List<IntPoint> minutiae) {
		Map<IntPoint, List<IntPoint>> linking = new HashMap<>();
		for (IntPoint minutiaPos : minutiae) {
			List<IntPoint> ownLinks = null;
			for (IntPoint neighborRelative : IntPoint.CORNER_NEIGHBORS) {
				IntPoint neighborPos = minutiaPos.plus(neighborRelative);
				if (linking.containsKey(neighborPos)) {
					List<IntPoint> neighborLinks = linking.get(neighborPos);
					if (neighborLinks != ownLinks) {
						if (ownLinks != null) {
							neighborLinks.addAll(ownLinks);
							for (IntPoint mergedPos : ownLinks)
								linking.put(mergedPos, neighborLinks);
						}
						ownLinks = neighborLinks;
					}
				}
			}
			if (ownLinks == null)
				ownLinks = new ArrayList<>();
			ownLinks.add(minutiaPos);
			linking.put(minutiaPos, ownLinks);
		}
		return linking;
	}
	private static Map<IntPoint, SkeletonMinutia> minutiaCenters(Skeleton skeleton, Map<IntPoint, List<IntPoint>> linking) {
		Map<IntPoint, SkeletonMinutia> centers = new HashMap<>();
		for (IntPoint currentPos : linking.keySet().stream().sorted().collect(toList())) {
			List<IntPoint> linkedMinutiae = linking.get(currentPos);
			IntPoint primaryPos = linkedMinutiae.get(0);
			if (!centers.containsKey(primaryPos)) {
				IntPoint sum = IntPoint.ZERO;
				for (IntPoint linkedPos : linkedMinutiae)
					sum = sum.plus(linkedPos);
				IntPoint center = new IntPoint(sum.x / linkedMinutiae.size(), sum.y / linkedMinutiae.size());
				SkeletonMinutia minutia = new SkeletonMinutia(center);
				skeleton.addMinutia(minutia);
				centers.put(primaryPos, minutia);
			}
			centers.put(currentPos, centers.get(primaryPos));
		}
		return centers;
	}
	private static void traceRidges(BooleanMatrix thinned, Map<IntPoint, SkeletonMinutia> minutiaePoints) {
		Map<IntPoint, SkeletonRidge> leads = new HashMap<>();
		for (IntPoint minutiaPoint : minutiaePoints.keySet().stream().sorted().collect(toList())) {
			for (IntPoint startRelative : IntPoint.CORNER_NEIGHBORS) {
				IntPoint start = minutiaPoint.plus(startRelative);
				if (thinned.get(start, false) && !minutiaePoints.containsKey(start) && !leads.containsKey(start)) {
					SkeletonRidge ridge = new SkeletonRidge();
					ridge.points.add(minutiaPoint);
					ridge.points.add(start);
					IntPoint previous = minutiaPoint;
					IntPoint current = start;
					do {
						IntPoint next = IntPoint.ZERO;
						for (IntPoint nextRelative : IntPoint.CORNER_NEIGHBORS) {
							next = current.plus(nextRelative);
							if (thinned.get(next, false) && !next.equals(previous))
								break;
						}
						previous = current;
						current = next;
						ridge.points.add(current);
					} while (!minutiaePoints.containsKey(current));
					IntPoint end = current;
					ridge.start(minutiaePoints.get(minutiaPoint));
					ridge.end(minutiaePoints.get(end));
					leads.put(ridge.points.get(1), ridge);
					leads.put(ridge.reversed.points.get(1), ridge);
				}
			}
		}
	}
	private static void fixLinkingGaps(Skeleton skeleton) {
		for (SkeletonMinutia minutia : skeleton.minutiae) {
			for (SkeletonRidge ridge : minutia.ridges) {
				if (!ridge.points.get(0).equals(minutia.position)) {
					IntPoint[] filling = ridge.points.get(0).lineTo(minutia.position);
					for (int i = 1; i < filling.length; ++i)
						ridge.reversed.points.add(filling[i]);
				}
			}
		}
	}
	public static Skeleton trace(BooleanMatrix thinned, SkeletonType type) {
		var skeleton = new Skeleton(type, thinned.size());
		var minutiaPoints = findMinutiae(thinned);
		var linking = linkNeighboringMinutiae(minutiaPoints);
		var minutiaMap = minutiaCenters(skeleton, linking);
		traceRidges(thinned, minutiaMap);
		fixLinkingGaps(skeleton);
		// https://sourceafis.machinezoo.com/transparency/traced-skeleton
		TransparencySink.current().logSkeleton("traced-skeleton", skeleton);
		return skeleton;
	}
}
