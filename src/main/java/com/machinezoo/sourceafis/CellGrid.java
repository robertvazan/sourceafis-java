// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class CellGrid {
	final int[] allX;
	final int[] allY;
	CellGrid(int width, int height) {
		allX = new int[width];
		allY = new int[height];
	}
	CellGrid(Cell size) {
		this(size.x, size.y);
	}
	Cell get(int x, int y) {
		return new Cell(allX[x], allY[y]);
	}
	Cell get(Cell at) {
		return get(at.x, at.y);
	}
}
