// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class JsonGrid {
	int[] x;
	int[] y;
	JsonGrid(CellGrid grid) {
		x = grid.allX;
		y = grid.allY;
	}
}
