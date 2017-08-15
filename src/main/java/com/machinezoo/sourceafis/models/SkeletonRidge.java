// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

import java.util.*;

public class SkeletonRidge {
	public final List<Cell> points;
	public final SkeletonRidge reversed;
	private SkeletonMinutia startMinutia;
	private SkeletonMinutia endMinutia;
	public SkeletonRidge() {
		points = new CircularList<>();
		reversed = new SkeletonRidge(this);
	}
	public SkeletonRidge(SkeletonRidge reversed) {
		points = new ReversedList<>(reversed.points);
		this.reversed = reversed;
	}
	public SkeletonMinutia start() {
		return startMinutia;
	}
	public void start(SkeletonMinutia value) {
		if (startMinutia != value) {
			if (startMinutia != null) {
				SkeletonMinutia detachFrom = startMinutia;
				startMinutia = null;
				detachFrom.detachStart(this);
			}
			startMinutia = value;
			if (startMinutia != null)
				startMinutia.attachStart(this);
			reversed.endMinutia = value;
		}
	}
	public SkeletonMinutia end() {
		return endMinutia;
	}
	public void end(SkeletonMinutia value) {
		if (endMinutia != value) {
			endMinutia = value;
			reversed.start(value);
		}
	}
	public void detach() {
		start(null);
		end(null);
	}
	public double direction(FingerprintContext context) {
		int first = context.ridgeDirectionSkip;
		int last = context.ridgeDirectionSkip + context.ridgeDirectionSample - 1;
		if (last >= points.size()) {
			int shift = last - points.size() + 1;
			last -= shift;
			first -= shift;
		}
		if (first < 0)
			first = 0;
		return Angle.atan(points.get(first), points.get(last));
	}
}
