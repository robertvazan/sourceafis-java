// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.extractor.skeletons;

import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.primitives.*;
import com.machinezoo.sourceafis.transparency.*;

public class BinaryThinning {
	private static enum NeighborhoodType {
		SKELETON,
		ENDING,
		REMOVABLE
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
	public static BooleanMatrix thin(BooleanMatrix input, SkeletonType type) {
		var neighborhoodTypes = neighborhoodTypes();
		var size = input.size();
		var partial = new BooleanMatrix(size);
		for (int y = 1; y < size.y - 1; ++y)
			for (int x = 1; x < size.x - 1; ++x)
				partial.set(x, y, input.get(x, y));
		var thinned = new BooleanMatrix(size);
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
		TransparencySink.current().log(type.prefix + "thinned-skeleton", thinned);
		return thinned;
	}
}
