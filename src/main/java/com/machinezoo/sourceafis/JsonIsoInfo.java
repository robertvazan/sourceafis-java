package com.machinezoo.sourceafis;

class JsonIsoInfo {
	int width;
	int height;
	int xPixelsPerCM;
	int yPixelsPerCM;
	JsonIsoInfo(int width, int height, int xPixelsPerCM, int yPixelsPerCM) {
		this.width = width;
		this.height = height;
		this.xPixelsPerCM = xPixelsPerCM;
		this.yPixelsPerCM = yPixelsPerCM;
	}
}
