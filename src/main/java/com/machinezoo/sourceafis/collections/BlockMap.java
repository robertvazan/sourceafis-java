// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.collections;

import com.machinezoo.sourceafis.scalars.*;

public class BlockMap {
	public final Cell pixelCount;
	public final Cell blockCount;
	public final Cell cornerCount;
	public final CellGrid corners;
	public final BlockGrid blockAreas;
	public final CellGrid blockCenters;
	public final BlockGrid cornerAreas;
	public BlockMap(int width, int height, int maxBlockSize) {
		pixelCount = new Cell(width, height);
		blockCount = new Cell(
			Integers.roundUpDiv(pixelCount.x, maxBlockSize),
			Integers.roundUpDiv(pixelCount.y, maxBlockSize));
		cornerCount = new Cell(blockCount.x + 1, blockCount.y + 1);
		corners = initCorners();
		blockAreas = new BlockGrid(corners);
		blockCenters = initBlockCenters();
		cornerAreas = initCornerAreas();
	}
	CellGrid initCorners() {
		CellGrid grid = new CellGrid(cornerCount);
		for (int y = 0; y < cornerCount.y; ++y)
			grid.allY[y] = y * pixelCount.y / blockCount.y;
		for (int x = 0; x < cornerCount.x; ++x)
			grid.allX[x] = x * pixelCount.x / blockCount.x;
		return grid;
	}
	CellGrid initBlockCenters() {
		CellGrid grid = new CellGrid(blockCount);
		for (int y = 0; y < blockCount.y; ++y)
			grid.allY[y] = blockAreas.get(0, y).center().y;
		for (int x = 0; x < blockCount.x; ++x)
			grid.allX[x] = blockAreas.get(x, 0).center().x;
		return grid;
	}
	BlockGrid initCornerAreas() {
		CellGrid grid = new CellGrid(cornerCount.x + 1, cornerCount.y + 1);
		grid.allY[0] = 0;
		for (int y = 0; y < blockCount.y; ++y)
			grid.allY[y + 1] = blockCenters.get(0, y).y;
		grid.allY[blockCount.y + 1] = pixelCount.y;
		grid.allX[0] = 0;
		for (int x = 0; x < blockCount.x; ++x)
			grid.allX[x + 1] = blockCenters.get(x, 0).x;
		grid.allX[blockCount.x + 1] = pixelCount.x;
		return new BlockGrid(grid);
	}
	public static class CellGrid {
		public final int[] allX;
		public final int[] allY;
		CellGrid(int width, int height) {
			allX = new int[width];
			allY = new int[height];
		}
		CellGrid(Cell size) {
			this(size.x, size.y);
		}
		public Cell get(int x, int y) {
			return new Cell(allX[x], allY[y]);
		}
		public Cell get(Cell at) {
			return get(at.x, at.y);
		}
	}
	public static class BlockGrid {
		public final CellGrid corners;
		BlockGrid(CellGrid corners) {
			this.corners = corners;
		}
		public Block get(int x, int y) {
			return Block.between(corners.get(x, y), corners.get(x + 1, y + 1));
		}
		public Block get(Cell at) {
			return get(at.x, at.y);
		}
	}
}
