// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class BlockGrid {
	final IntPoint blocks;
	final IntPoint corners;
	final int[] x;
	final int[] y;
	BlockGrid(IntPoint size) {
		blocks = size;
		corners = new IntPoint(size.x + 1, size.y + 1);
		x = new int[size.x + 1];
		y = new int[size.y + 1];
	}
	BlockGrid(int width, int height) {
		this(new IntPoint(width, height));
	}
	IntPoint corner(int atX, int atY) {
		return new IntPoint(x[atX], y[atY]);
	}
	IntPoint corner(IntPoint at) {
		return corner(at.x, at.y);
	}
	IntRect block(int atX, int atY) {
		return IntRect.between(corner(atX, atY), corner(atX + 1, atY + 1));
	}
	IntRect block(IntPoint at) {
		return block(at.x, at.y);
	}
}
