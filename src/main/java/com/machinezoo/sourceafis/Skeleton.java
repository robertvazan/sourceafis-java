// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.nio.*;
import java.util.*;

class Skeleton {
	private final FingerprintTransparency logger;
	final SkeletonType type;
	final Cell size;
	final List<SkeletonMinutia> minutiae = new ArrayList<>();
	Skeleton(BooleanMap binary, SkeletonType type, FingerprintTransparency logger) {
		this.type = type;
		this.logger = logger;
		// https://sourceafis.machinezoo.com/transparency/binarized-skeleton
		logger.logBinarizedSkeleton(type, binary);
		size = binary.size();
		BooleanMap thinned = thin(binary);
		List<Cell> minutiaPoints = findMinutiae(thinned);
		Map<Cell, List<Cell>> linking = linkNeighboringMinutiae(minutiaPoints);
		Map<Cell, SkeletonMinutia> minutiaMap = minutiaCenters(linking);
		traceRidges(thinned, minutiaMap);
		fixLinkingGaps();
		// https://sourceafis.machinezoo.com/transparency/traced-skeleton
		logger.logTracedSkeleton(this);
		filter();
	}
	private enum NeighborhoodType {
		Skeleton,
		Ending,
		Removable
	}
	private BooleanMap thin(BooleanMap input) {
		NeighborhoodType[] neighborhoodTypes = neighborhoodTypes();
		BooleanMap partial = new BooleanMap(size);
		for (int y = 1; y < size.y - 1; ++y)
			for (int x = 1; x < size.x - 1; ++x)
				partial.set(x, y, input.get(x, y));
		BooleanMap thinned = new BooleanMap(size);
		boolean removedAnything = true;
		for (int i = 0; i < Parameters.thinningIterations && removedAnything; ++i) {
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
								if (neighborhoodTypes[neighbors] == NeighborhoodType.Removable
									|| neighborhoodTypes[neighbors] == NeighborhoodType.Ending
										&& isFalseEnding(partial, new Cell(x, y))) {
									removedAnything = true;
									partial.set(x, y, false);
								} else
									thinned.set(x, y, true);
							}
		}
		// https://sourceafis.machinezoo.com/transparency/thinned-skeleton
		logger.logThinnedSkeleton(type, thinned);
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
				types[mask] = NeighborhoodType.Ending;
			else if (!diagonal && !horizontal && !vertical)
				types[mask] = NeighborhoodType.Removable;
			else
				types[mask] = NeighborhoodType.Skeleton;
		}
		return types;
	}
	private static boolean isFalseEnding(BooleanMap binary, Cell ending) {
		for (Cell relativeNeighbor : Cell.cornerNeighbors) {
			Cell neighbor = ending.plus(relativeNeighbor);
			if (binary.get(neighbor)) {
				int count = 0;
				for (Cell relative2 : Cell.cornerNeighbors)
					if (binary.get(neighbor.plus(relative2), false))
						++count;
				return count > 2;
			}
		}
		return false;
	}
	private List<Cell> findMinutiae(BooleanMap thinned) {
		List<Cell> result = new ArrayList<>();
		for (Cell at : size)
			if (thinned.get(at)) {
				int count = 0;
				for (Cell relative : Cell.cornerNeighbors)
					if (thinned.get(at.plus(relative), false))
						++count;
				if (count == 1 || count > 2)
					result.add(at);
			}
		return result;
	}
	private static Map<Cell, List<Cell>> linkNeighboringMinutiae(List<Cell> minutiae) {
		Map<Cell, List<Cell>> linking = new HashMap<>();
		for (Cell minutiaPos : minutiae) {
			List<Cell> ownLinks = null;
			for (Cell neighborRelative : Cell.cornerNeighbors) {
				Cell neighborPos = minutiaPos.plus(neighborRelative);
				if (linking.containsKey(neighborPos)) {
					List<Cell> neighborLinks = linking.get(neighborPos);
					if (neighborLinks != ownLinks) {
						if (ownLinks != null) {
							neighborLinks.addAll(ownLinks);
							for (Cell mergedPos : ownLinks)
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
	private Map<Cell, SkeletonMinutia> minutiaCenters(Map<Cell, List<Cell>> linking) {
		Map<Cell, SkeletonMinutia> centers = new HashMap<>();
		for (Cell currentPos : linking.keySet()) {
			List<Cell> linkedMinutiae = linking.get(currentPos);
			Cell primaryPos = linkedMinutiae.get(0);
			if (!centers.containsKey(primaryPos)) {
				Cell sum = Cell.zero;
				for (Cell linkedPos : linkedMinutiae)
					sum = sum.plus(linkedPos);
				Cell center = new Cell(sum.x / linkedMinutiae.size(), sum.y / linkedMinutiae.size());
				SkeletonMinutia minutia = new SkeletonMinutia(center);
				addMinutia(minutia);
				centers.put(primaryPos, minutia);
			}
			centers.put(currentPos, centers.get(primaryPos));
		}
		return centers;
	}
	private void traceRidges(BooleanMap thinned, Map<Cell, SkeletonMinutia> minutiaePoints) {
		Map<Cell, SkeletonRidge> leads = new HashMap<>();
		for (Cell minutiaPoint : minutiaePoints.keySet()) {
			for (Cell startRelative : Cell.cornerNeighbors) {
				Cell start = minutiaPoint.plus(startRelative);
				if (thinned.get(start, false) && !minutiaePoints.containsKey(start) && !leads.containsKey(start)) {
					SkeletonRidge ridge = new SkeletonRidge();
					ridge.points.add(minutiaPoint);
					ridge.points.add(start);
					Cell previous = minutiaPoint;
					Cell current = start;
					do {
						Cell next = Cell.zero;
						for (Cell nextRelative : Cell.cornerNeighbors) {
							next = current.plus(nextRelative);
							if (thinned.get(next, false) && !next.equals(previous))
								break;
						}
						previous = current;
						current = next;
						ridge.points.add(current);
					} while (!minutiaePoints.containsKey(current));
					Cell end = current;
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
					Cell[] filling = ridge.points.get(0).lineTo(minutia.position);
					for (int i = 1; i < filling.length; ++i)
						ridge.reversed.points.add(filling[i]);
				}
			}
		}
	}
	private void filter() {
		removeDots();
		// https://sourceafis.machinezoo.com/transparency/removed-dots
		logger.logRemovedDots(this);
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
						if (end.ridges.size() == 3 && arm1.points.size() <= Parameters.maxPoreArm && arm2.points.size() <= Parameters.maxPoreArm) {
							arm1.detach();
							arm2.detach();
							SkeletonRidge merged = new SkeletonRidge();
							merged.start(minutia);
							merged.end(end);
							for (Cell point : minutia.position.lineTo(end.position))
								merged.points.add(point);
						}
						break;
					}
				}
			}
		}
		removeKnots();
		// https://sourceafis.machinezoo.com/transparency/removed-pores
		logger.logRemovedPores(this);
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
			if (end1.ridges.size() == 1 && end1.ridges.get(0).points.size() >= Parameters.shortestJoinedEnding)
				for (SkeletonMinutia end2 : minutiae)
					if (end2 != end1 && end2.ridges.size() == 1 && end1.ridges.get(0).end() != end2
						&& end2.ridges.get(0).points.size() >= Parameters.shortestJoinedEnding && isWithinGapLimits(end1, end2)) {
						Gap gap = new Gap();
						gap.distance = end1.position.minus(end2.position).lengthSq();
						gap.end1 = end1;
						gap.end2 = end2;
						queue.add(gap);
					}
		BooleanMap shadow = shadow();
		while (!queue.isEmpty()) {
			Gap gap = queue.remove();
			if (gap.end1.ridges.size() == 1 && gap.end2.ridges.size() == 1) {
				Cell[] line = gap.end1.position.lineTo(gap.end2.position);
				if (!isRidgeOverlapping(line, shadow))
					addGapRidge(shadow, gap, line);
			}
		}
		removeKnots();
		// https://sourceafis.machinezoo.com/transparency/removed-gaps
		logger.logRemovedGaps(this);
	}
	private boolean isWithinGapLimits(SkeletonMinutia end1, SkeletonMinutia end2) {
		int distanceSq = end1.position.minus(end2.position).lengthSq();
		if (distanceSq <= Integers.sq(Parameters.maxRuptureSize))
			return true;
		if (distanceSq > Integers.sq(Parameters.maxGapSize))
			return false;
		double gapDirection = Angle.atan(end1.position, end2.position);
		double direction1 = Angle.atan(end1.position, angleSampleForGapRemoval(end1));
		if (Angle.distance(direction1, Angle.opposite(gapDirection)) > Parameters.maxGapAngle)
			return false;
		double direction2 = Angle.atan(end2.position, angleSampleForGapRemoval(end2));
		if (Angle.distance(direction2, gapDirection) > Parameters.maxGapAngle)
			return false;
		return true;
	}
	private Cell angleSampleForGapRemoval(SkeletonMinutia minutia) {
		SkeletonRidge ridge = minutia.ridges.get(0);
		if (Parameters.gapAngleOffset < ridge.points.size())
			return ridge.points.get(Parameters.gapAngleOffset);
		else
			return ridge.end().position;
	}
	private boolean isRidgeOverlapping(Cell[] line, BooleanMap shadow) {
		for (int i = Parameters.toleratedGapOverlap; i < line.length - Parameters.toleratedGapOverlap; ++i)
			if (shadow.get(line[i]))
				return true;
		return false;
	}
	private static void addGapRidge(BooleanMap shadow, Gap gap, Cell[] line) {
		SkeletonRidge ridge = new SkeletonRidge();
		for (Cell point : line)
			ridge.points.add(point);
		ridge.start(gap.end1);
		ridge.end(gap.end2);
		for (Cell point : line)
			shadow.set(point, true);
	}
	private void removeTails() {
		for (SkeletonMinutia minutia : minutiae) {
			if (minutia.ridges.size() == 1 && minutia.ridges.get(0).end().ridges.size() >= 3)
				if (minutia.ridges.get(0).points.size() < Parameters.minTailLength)
					minutia.ridges.get(0).detach();
		}
		removeDots();
		removeKnots();
		// https://sourceafis.machinezoo.com/transparency/removed-tails
		logger.logRemovedTails(this);
	}
	private void removeFragments() {
		for (SkeletonMinutia minutia : minutiae)
			if (minutia.ridges.size() == 1) {
				SkeletonRidge ridge = minutia.ridges.get(0);
				if (ridge.end().ridges.size() == 1 && ridge.points.size() < Parameters.minFragmentLength)
					ridge.detach();
			}
		removeDots();
		// https://sourceafis.machinezoo.com/transparency/removed-fragments
		logger.logRemovedFragments(this);
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
				for (Cell point : removed.points)
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
	private BooleanMap shadow() {
		BooleanMap shadow = new BooleanMap(size);
		for (SkeletonMinutia minutia : minutiae) {
			shadow.set(minutia.position, true);
			for (SkeletonRidge ridge : minutia.ridges)
				if (ridge.start().position.y <= ridge.end().position.y)
					for (Cell point : ridge.points)
						shadow.set(point, true);
		}
		return shadow;
	}
	ByteBuffer serialize() {
		ByteBuffer buffer = ByteBuffer.allocate(minutiae.stream().mapToInt(m -> m.serializedSize()).sum());
		for (SkeletonMinutia minutia : minutiae)
			minutia.write(buffer);
		buffer.flip();
		return buffer;
	}
}
