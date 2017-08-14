// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

import lombok.*;

@RequiredArgsConstructor public class BlockGrid {
	public final CellGrid corners;
	public Block get(int x, int y) {
		return Block.between(corners.get(x, y), corners.get(x + 1, y + 1));
	}
	public Block get(Cell at) {
		return get(at.x, at.y);
	}
}
