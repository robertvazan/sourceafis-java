// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class BlockGrid {
	final Cell blocks;
	final Cell corners;
	final int[] x;
	final int[] y;
	BlockGrid(Cell size) {
		blocks = size;
		corners = new Cell(size.x + 1, size.y + 1);
		x = new int[size.x + 1];
		y = new int[size.y + 1];
	}
	BlockGrid(int width, int height) {
		this(new Cell(width, height));
	}
	Cell corner(int atX, int atY) {
		return new Cell(x[atX], y[atY]);
	}
	Cell corner(Cell at) {
		return corner(at.x, at.y);
	}
	Block block(int atX, int atY) {
		return Block.between(corner(atX, atY), corner(atX + 1, atY + 1));
	}
	Block block(Cell at) {
		return block(at.x, at.y);
	}
}
