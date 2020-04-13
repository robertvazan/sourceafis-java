// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.util.*;

class Skeleton {
	final SkeletonType type;
	final IntPoint size;
	final List<SkeletonMinutia> minutiae = new ArrayList<>();
	Skeleton(BooleanMatrix binary, SkeletonType type) {
		this.type = type;
		// https://sourceafis.machinezoo.com/transparency/binarized-skeleton
		FingerprintTransparency.current().log(type.prefix + "binarized-skeleton", binary);
		size = binary.size();
		BooleanMatrix thinned = thin(binary);
		List<IntPoint> minutiaPoints = findMinutiae(thinned);
		Map<IntPoint, List<IntPoint>> linking = linkNeighboringMinutiae(minutiaPoints);
		Map<IntPoint, SkeletonMinutia> minutiaMap = minutiaCenters(linking);
		traceRidges(thinned, minutiaMap);
		fixLinkingGaps();
		// https://sourceafis.machinezoo.com/transparency/traced-skeleton
		FingerprintTransparency.current().logSkeleton("traced-skeleton", this);
		filter();
	}
	private enum NeighborhoodType {
		SKELETON,
		ENDING,
		REMOVABLE
	}
	private BooleanMatrix thin(BooleanMatrix input) {
		NeighborhoodType[] neighborhoodTypes = neighborhoodTypes();
		BooleanMatrix partial = new BooleanMatrix(size);
		for (int y = 1; y < size.y - 1; ++y)
			for (int x = 1; x < size.x - 1; ++x)
				partial.set(x, y, input.get(x, y));
		BooleanMatrix thinned = new BooleanMatrix(size);
		boolean removedAnything = true;
		for (int i = 0; i < Parameters.THINNING_ITERATIONS && removedAnything; ++i) {
			removedAnything = false;
			for (int evenY = 0; evenY < 2; ++evenY)
				for (int evenX = 0; evenX < 2; ++evenX)
					for (int y = 1 + evenY; y < size.y - 1; y += 2)
						for (int x = 1 + evenX; x < size.x - 1; x += 2)
							if (partial.get(x, y) && !thinned.get(x, y) && !(partial.get(x, y - 1) && partial.get(x, y + 1) && partial.get(x - 1, y) && partial.get(x + 1, y))) {
								int neighbors = (partial.get(x + 1, y + 1) ? 128 : 0)
									| (partial.get(x, y + 1) ? 64 : 0)
									| (partial.get(x - 1, y + 1) ? 32 : 0)
									| (partial.get(x + 1, y) ? 16 : 0)
									| (partial.get(x - 1, y) ? 8 : 0)
									| (partial.get(x + 1, y - 1) ? 4 : 0)
									| (partial.get(x, y - 1) ? 2 : 0)
									| (partial.get(x - 1, y - 1) ? 1 : 0);
								if (neighborhoodTypes[neighbors] == NeighborhoodType.REMOVABLE
									|| neighborhoodTypes[neighbors] == NeighborhoodType.ENDING
										&& isFalseEnding(partial, new IntPoint(x, y))) {
									removedAnything = true;
									partial.set(x, y, false);
								} else
									thinned.set(x, y, true);
							}
		}
		// https://sourceafis.machinezoo.com/transparency/thinned-skeleton
		FingerprintTransparency.current().log(type.prefix + "thinned-skeleton", thinned);
		return thinned;
	}
	private static NeighborhoodType[] neighborhoodTypes() {
		NeighborhoodType[] types = new NeighborhoodType[256];
		for (int mask = 0; mask < 256; ++mask) {
			boolean TL = (mask & 1) != 0;
			boolean TC = (mask & 2) != 0;
			boolean TR = (mask & 4) != 0;
			boolean CL = (mask & 8) != 0;
			boolean CR = (mask & 16) != 0;
			boolean BL = (mask & 32) != 0;
			boolean BC = (mask & 64) != 0;
			boolean BR = (mask & 128) != 0;
			int count = Integer.bitCount(mask);
			boolean diagonal = !TC && !CL && TL || !CL && !BC && BL || !BC && !CR && BR || !CR && !TC && TR;
			boolean horizontal = !TC && !BC && (TR || CR || BR) && (TL || CL || BL);
			boolean vertical = !CL && !CR && (TL || TC || TR) && (BL || BC || BR);
			boolean end = (count == 1);
			if (end)
				types[mask] = NeighborhoodType.ENDING;
			else if (!diagonal && !horizontal && !vertical)
				types[mask] = NeighborhoodType.REMOVABLE;
			else
				types[mask] = NeighborhoodType.SKELETON;
		}
		return types;
	}
	private static boolean isFalseEnding(BooleanMatrix binary, IntPoint ending) {
		for (IntPoint relativeNeighbor : IntPoint.CORNER_NEIGHBORS) {
			IntPoint neighbor = ending.plus(relativeNeighbor);
			if (binary.get(neighbor)) {
				int count = 0;
				for (IntPoint relative2 : IntPoint.CORNER_NEIGHBORS)
					if (binary.get(neighbor.plus(relative2), false))
						++count;
				return count > 2;
			}
		}
		return false;
	}
	private List<IntPoint> findMinutiae(BooleanMatrix thinned) {
		List<IntPoint> result = new ArrayList<>();
		for (IntPoint at : size)
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
	private Map<IntPoint, SkeletonMinutia> minutiaCenters(Map<IntPoint, List<IntPoint>> linking) {
		Map<IntPoint, SkeletonMinutia> centers = new HashMap<>();
		for (IntPoint currentPos : linking.keySet()) {
			List<IntPoint> linkedMinutiae = linking.get(currentPos);
			IntPoint primaryPos = linkedMinutiae.get(0);
			if (!centers.containsKey(primaryPos)) {
				IntPoint sum = IntPoint.ZERO;
				for (IntPoint linkedPos : linkedMinutiae)
					sum = sum.plus(linkedPos);
				IntPoint center = new IntPoint(sum.x / linkedMinutiae.size(), sum.y / linkedMinutiae.size());
				SkeletonMinutia minutia = new SkeletonMinutia(center);
				addMinutia(minutia);
				centers.put(primaryPos, minutia);
			}
			centers.put(currentPos, centers.get(primaryPos));
		}
		return centers;
	}
	private void traceRidges(BooleanMatrix thinned, Map<IntPoint, SkeletonMinutia> minutiaePoints) {
		Map<IntPoint, SkeletonRidge> leads = new HashMap<>();
		for (IntPoint minutiaPoint : minutiaePoints.keySet()) {
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
	private void fixLinkingGaps() {
		for (SkeletonMinutia minutia : minutiae) {
			for (SkeletonRidge ridge : minutia.ridges) {
				if (!ridge.points.get(0).equals(minutia.position)) {
					IntPoint[] filling = ridge.points.get(0).lineTo(minutia.position);
					for (int i = 1; i < filling.length; ++i)
						ridge.reversed.points.add(filling[i]);
				}
			}
		}
	}
	private void filter() {
		removeDots();
		// https://sourceafis.machinezoo.com/transparency/removed-dots
		FingerprintTransparency.current().logSkeleton("removed-dots", this);
		removePores();
		removeGaps();
		removeTails();
		removeFragments();
	}
	private void removeDots() {
		List<SkeletonMinutia> removed = new ArrayList<>();
		for (SkeletonMinutia minutia : minutiae)
			if (minutia.ridges.isEmpty())
				removed.add(minutia);
		for (SkeletonMinutia minutia : removed)
			removeMinutia(minutia);
	}
	private void removePores() {
		for (SkeletonMinutia minutia : minutiae) {
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
		removeKnots();
		// https://sourceafis.machinezoo.com/transparency/removed-pores
		FingerprintTransparency.current().logSkeleton("removed-pores", this);
	}
	private static class Gap implements Comparable<Gap> {
		int distance;
		SkeletonMinutia end1;
		SkeletonMinutia end2;
		@Override public int compareTo(Gap other) {
			return Integer.compare(distance, other.distance);
		}
	}
	private void removeGaps() {
		PriorityQueue<Gap> queue = new PriorityQueue<>();
		for (SkeletonMinutia end1 : minutiae)
			if (end1.ridges.size() == 1 && end1.ridges.get(0).points.size() >= Parameters.SHORTEST_JOINED_ENDING)
				for (SkeletonMinutia end2 : minutiae)
					if (end2 != end1 && end2.ridges.size() == 1 && end1.ridges.get(0).end() != end2
						&& end2.ridges.get(0).points.size() >= Parameters.SHORTEST_JOINED_ENDING && isWithinGapLimits(end1, end2)) {
						Gap gap = new Gap();
						gap.distance = end1.position.minus(end2.position).lengthSq();
						gap.end1 = end1;
						gap.end2 = end2;
						queue.add(gap);
					}
		BooleanMatrix shadow = shadow();
		while (!queue.isEmpty()) {
			Gap gap = queue.remove();
			if (gap.end1.ridges.size() == 1 && gap.end2.ridges.size() == 1) {
				IntPoint[] line = gap.end1.position.lineTo(gap.end2.position);
				if (!isRidgeOverlapping(line, shadow))
					addGapRidge(shadow, gap, line);
			}
		}
		removeKnots();
		// https://sourceafis.machinezoo.com/transparency/removed-gaps
		FingerprintTransparency.current().logSkeleton("removed-gaps", this);
	}
	private boolean isWithinGapLimits(SkeletonMinutia end1, SkeletonMinutia end2) {
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
	private IntPoint angleSampleForGapRemoval(SkeletonMinutia minutia) {
		SkeletonRidge ridge = minutia.ridges.get(0);
		if (Parameters.GAP_ANGLE_OFFSET < ridge.points.size())
			return ridge.points.get(Parameters.GAP_ANGLE_OFFSET);
		else
			return ridge.end().position;
	}
	private boolean isRidgeOverlapping(IntPoint[] line, BooleanMatrix shadow) {
		for (int i = Parameters.TOLERATED_GAP_OVERLAP; i < line.length - Parameters.TOLERATED_GAP_OVERLAP; ++i)
			if (shadow.get(line[i]))
				return true;
		return false;
	}
	private static void addGapRidge(BooleanMatrix shadow, Gap gap, IntPoint[] line) {
		SkeletonRidge ridge = new SkeletonRidge();
		for (IntPoint point : line)
			ridge.points.add(point);
		ridge.start(gap.end1);
		ridge.end(gap.end2);
		for (IntPoint point : line)
			shadow.set(point, true);
	}
	private void removeTails() {
		for (SkeletonMinutia minutia : minutiae) {
			if (minutia.ridges.size() == 1 && minutia.ridges.get(0).end().ridges.size() >= 3)
				if (minutia.ridges.get(0).points.size() < Parameters.MIN_TAIL_LENGTH)
					minutia.ridges.get(0).detach();
		}
		removeDots();
		removeKnots();
		// https://sourceafis.machinezoo.com/transparency/removed-tails
		FingerprintTransparency.current().logSkeleton("removed-tails", this);
	}
	private void removeFragments() {
		for (SkeletonMinutia minutia : minutiae)
			if (minutia.ridges.size() == 1) {
				SkeletonRidge ridge = minutia.ridges.get(0);
				if (ridge.end().ridges.size() == 1 && ridge.points.size() < Parameters.MIN_FRAGMENT_LENGTH)
					ridge.detach();
			}
		removeDots();
		// https://sourceafis.machinezoo.com/transparency/removed-fragments
		FingerprintTransparency.current().logSkeleton("removed-fragments", this);
	}
	private void removeKnots() {
		for (SkeletonMinutia minutia : minutiae) {
			if (minutia.ridges.size() == 2 && minutia.ridges.get(0).reversed != minutia.ridges.get(1)) {
				SkeletonRidge extended = minutia.ridges.get(0).reversed;
				SkeletonRidge removed = minutia.ridges.get(1);
				if (extended.points.size() < removed.points.size()) {
					SkeletonRidge tmp = extended;
					extended = removed;
					removed = tmp;
					extended = extended.reversed;
					removed = removed.reversed;
				}
				extended.points.remove(extended.points.size() - 1);
				for (IntPoint point : removed.points)
					extended.points.add(point);
				extended.end(removed.end());
				removed.detach();
			}
		}
		removeDots();
	}
	private void addMinutia(SkeletonMinutia minutia) {
		minutiae.add(minutia);
	}
	private void removeMinutia(SkeletonMinutia minutia) {
		minutiae.remove(minutia);
	}
	private BooleanMatrix shadow() {
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
