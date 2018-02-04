// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import lombok.*;

@RequiredArgsConstructor class BlockGrid {
	final CellGrid corners;
	Block get(int x, int y) {
		return Block.between(corners.get(x, y), corners.get(x + 1, y + 1));
	}
	Block get(Cell at) {
		return get(at.x, at.y);
	}
}
