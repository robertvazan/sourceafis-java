// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class PointMap {
	final int width;
	final int height;
	final double[] arrayX;
	final double[] arrayY;
	PointMap(int width, int height) {
		this.width = width;
		this.height = height;
		arrayX = new double[width * height];
		arrayY = new double[width * height];
	}
	PointMap(Cell size) {
		this(size.x, size.y);
	}
	Point get(int x, int y) {
		int i = offset(x, y);
		return new Point(arrayX[i], arrayY[i]);
	}
	Point get(Cell at) {
		return get(at.x, at.y);
	}
	void set(int x, int y, double px, double py) {
		int i = offset(x, y);
		arrayX[i] = px;
		arrayY[i] = py;
	}
	void set(int x, int y, Point point) {
		set(x, y, point.x, point.y);
	}
	void set(Cell at, Point point) {
		set(at.x, at.y, point);
	}
	void add(int x, int y, double px, double py) {
		int i = offset(x, y);
		arrayX[i] += px;
		arrayY[i] += py;
	}
	void add(int x, int y, Point point) {
		add(x, y, point.x, point.y);
	}
	void add(Cell at, Point point) {
		add(at.x, at.y, point);
	}
	int offset(int x, int y) {
		return y * width + x;
	}
}
