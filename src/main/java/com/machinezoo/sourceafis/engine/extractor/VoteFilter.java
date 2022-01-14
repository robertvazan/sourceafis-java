// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.extractor;

import java.util.stream.*;
import com.machinezoo.sourceafis.engine.primitives.*;

public class VoteFilter {
	public static BooleanMatrix vote(BooleanMatrix input, BooleanMatrix mask, int radius, double majority, int borderDistance) {
		IntPoint size = input.size();
		IntRect rect = new IntRect(borderDistance, borderDistance, size.x - 2 * borderDistance, size.y - 2 * borderDistance);
		int[] thresholds = IntStream.range(0, Integers.sq(2 * radius + 1) + 1).map(i -> (int)Math.ceil(majority * i)).toArray();
		IntMatrix counts = new IntMatrix(size);
		BooleanMatrix output = new BooleanMatrix(size);
		for (int y = rect.top(); y < rect.bottom(); ++y) {
			int superTop = y - radius - 1;
			int superBottom = y + radius;
			int yMin = Math.max(0, y - radius);
			int yMax = Math.min(size.y - 1, y + radius);
			int yRange = yMax - yMin + 1;
			for (int x = rect.left(); x < rect.right(); ++x)
				if (mask == null || mask.get(x, y)) {
					int left = x > 0 ? counts.get(x - 1, y) : 0;
					int top = y > 0 ? counts.get(x, y - 1) : 0;
					int diagonal = x > 0 && y > 0 ? counts.get(x - 1, y - 1) : 0;
					int xMin = Math.max(0, x - radius);
					int xMax = Math.min(size.x - 1, x + radius);
					int ones;
					if (left > 0 && top > 0 && diagonal > 0) {
						ones = top + left - diagonal - 1;
						int superLeft = x - radius - 1;
						int superRight = x + radius;
						if (superLeft >= 0 && superTop >= 0 && input.get(superLeft, superTop))
							++ones;
						if (superLeft >= 0 && superBottom < size.y && input.get(superLeft, superBottom))
							--ones;
						if (superRight < size.x && superTop >= 0 && input.get(superRight, superTop))
							--ones;
						if (superRight < size.x && superBottom < size.y && input.get(superRight, superBottom))
							++ones;
					} else {
						ones = 0;
						for (int ny = yMin; ny <= yMax; ++ny)
							for (int nx = xMin; nx <= xMax; ++nx)
								if (input.get(nx, ny))
									++ones;
					}
					counts.set(x, y, ones + 1);
					if (ones >= thresholds[yRange * (xMax - xMin + 1)])
						output.set(x, y, true);
				}
		}
		return output;
	}
}
