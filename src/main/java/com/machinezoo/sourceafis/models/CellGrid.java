// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class CellGrid {
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
