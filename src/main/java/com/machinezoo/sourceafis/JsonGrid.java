package com.machinezoo.sourceafis;

class JsonGrid {
	int[] x;
	int[] y;
	JsonGrid(CellGrid grid) {
		x = grid.allX;
		y = grid.allY;
	}
}