// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class PointMap {
	public final int width;
	public final int height;
	public final double[] arrayX;
	public final double[] arrayY;
	public PointMap(int width, int height) {
		this.width = width;
		this.height = height;
		arrayX = new double[width * height];
		arrayY = new double[width * height];
	}
	public PointMap(Cell size) {
		this(size.x, size.y);
	}
	public Point get(int x, int y) {
		int i = offset(x, y);
		return new Point(arrayX[i], arrayY[i]);
	}
	public Point get(Cell at) {
		return get(at.x, at.y);
	}
	public void set(int x, int y, double px, double py) {
		int i = offset(x, y);
		arrayX[i] = px;
		arrayY[i] = py;
	}
	public void set(int x, int y, Point point) {
		set(x, y, point.x, point.y);
	}
	public void set(Cell at, Point point) {
		set(at.x, at.y, point);
	}
	public void add(int x, int y, double px, double py) {
		int i = offset(x, y);
		arrayX[i] += px;
		arrayY[i] += py;
	}
	public void add(int x, int y, Point point) {
		add(x, y, point.x, point.y);
	}
	public void add(Cell at, Point point) {
		add(at.x, at.y, point);
	}
	int offset(int x, int y) {
		return y * width + x;
	}
}
