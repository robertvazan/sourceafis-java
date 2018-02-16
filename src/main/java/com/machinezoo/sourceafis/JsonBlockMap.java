// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class JsonBlockMap {
	Cell pixelCount;
	Cell blockCount;
	Cell cornerCount;
	JsonGrid corners;
	JsonGrid centers;
	JsonGrid cornerAreas;
	JsonBlockMap(BlockMap blocks) {
		pixelCount = blocks.pixelCount;
		blockCount = blocks.blockCount;
		cornerCount = blocks.cornerCount;
		corners = new JsonGrid(blocks.corners);
		centers = new JsonGrid(blocks.blockCenters);
		cornerAreas = new JsonGrid(blocks.cornerAreas.corners);
	}
}