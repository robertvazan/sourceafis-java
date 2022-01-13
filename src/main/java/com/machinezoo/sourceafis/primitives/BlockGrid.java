// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.primitives;

public class BlockGrid {
	public final IntPoint blocks;
	public final IntPoint corners;
	public final int[] x;
	public final int[] y;
	public BlockGrid(IntPoint size) {
		blocks = size;
		corners = new IntPoint(size.x + 1, size.y + 1);
		x = new int[size.x + 1];
		y = new int[size.y + 1];
	}
	public BlockGrid(int width, int height) {
		this(new IntPoint(width, height));
	}
	public IntPoint corner(int atX, int atY) {
		return new IntPoint(x[atX], y[atY]);
	}
	public IntPoint corner(IntPoint at) {
		return corner(at.x, at.y);
	}
	public IntRect block(int atX, int atY) {
		return IntRect.between(corner(atX, atY), corner(atX + 1, atY + 1));
	}
	public IntRect block(IntPoint at) {
		return block(at.x, at.y);
	}
}
