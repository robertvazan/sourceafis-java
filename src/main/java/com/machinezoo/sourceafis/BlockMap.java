// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class BlockMap {
	final Cell pixels;
	final BlockGrid primary;
	final BlockGrid secondary;
	BlockMap(int width, int height, int maxBlockSize) {
		pixels = new Cell(width, height);
		primary = new BlockGrid(new Cell(
			Integers.roundUpDiv(pixels.x, maxBlockSize),
			Integers.roundUpDiv(pixels.y, maxBlockSize)));
		for (int y = 0; y <= primary.blocks.y; ++y)
			primary.y[y] = y * pixels.y / primary.blocks.y;
		for (int x = 0; x <= primary.blocks.x; ++x)
			primary.x[x] = x * pixels.x / primary.blocks.x;
		secondary = new BlockGrid(primary.corners);
		secondary.y[0] = 0;
		for (int y = 0; y < primary.blocks.y; ++y)
			secondary.y[y + 1] = primary.block(0, y).center().y;
		secondary.y[secondary.blocks.y] = pixels.y;
		secondary.x[0] = 0;
		for (int x = 0; x < primary.blocks.x; ++x)
			secondary.x[x + 1] = primary.block(x, 0).center().x;
		secondary.x[secondary.blocks.x] = pixels.x;
	}
}
