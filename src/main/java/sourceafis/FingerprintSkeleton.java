package sourceafis;

import java.util.*;
import sourceafis.collections.*;
import sourceafis.scalars.*;

class FingerprintSkeleton {
	final Cell size;
	final List<SkeletonMinutia> minutiae = new ArrayList<>();
	FingerprintSkeleton(BooleanMap binary) {
		size = binary.size();
		BooleanMap thinned = thin(binary);
		List<Cell> minutiaPoints = findMinutiae(thinned);
		Map<Cell, List<Cell>> linking = linkNeighboringMinutiae(minutiaPoints);
		Map<Cell, SkeletonMinutia> minutiaMap = minutiaCenters(linking);
		traceRidges(thinned, minutiaMap);
	}
	enum NeighborhoodType {
		Skeleton,
		Ending,
		Removable
	}
	BooleanMap thin(BooleanMap input) {
		final int maxIterations = 26;
		NeighborhoodType[] neighborhoodTypes = neighborhoodTypes();
		BooleanMap partial = new BooleanMap(size);
		for (int y = 1; y < size.y - 1; ++y)
			for (int x = 1; x < size.x - 1; ++x)
				partial.set(x, y, input.get(x, y));
		BooleanMap thinned = new BooleanMap(size);
		boolean removedAnything = true;
		for (int i = 0; i < maxIterations && removedAnything; ++i) {
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
		return thinned;
	}
	static NeighborhoodType[] neighborhoodTypes() {
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
	static boolean isFalseEnding(BooleanMap binary, Cell ending) {
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
	List<Cell> findMinutiae(BooleanMap thinned) {
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
	static Map<Cell, List<Cell>> linkNeighboringMinutiae(List<Cell> minutiae) {
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
	Map<Cell, SkeletonMinutia> minutiaCenters(Map<Cell, List<Cell>> linking) {
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
				AddMinutia(minutia);
				centers.put(primaryPos, minutia);
			}
			centers.put(currentPos, centers.get(primaryPos));
		}
		return centers;
	}
	static void traceRidges(BooleanMap thinned, Map<Cell, SkeletonMinutia> minutiaePoints) {
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
	void AddMinutia(SkeletonMinutia minutia) {
		minutiae.add(minutia);
	}
}
