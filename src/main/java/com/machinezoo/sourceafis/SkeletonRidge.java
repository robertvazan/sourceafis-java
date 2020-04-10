// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.util.*;

class SkeletonRidge {
	final List<IntPoint> points;
	final SkeletonRidge reversed;
	private SkeletonMinutia startMinutia;
	private SkeletonMinutia endMinutia;
	SkeletonRidge() {
		points = new CircularList<>();
		reversed = new SkeletonRidge(this);
	}
	SkeletonRidge(SkeletonRidge reversed) {
		points = new ReversedList<>(reversed.points);
		this.reversed = reversed;
	}
	SkeletonMinutia start() {
		return startMinutia;
	}
	void start(SkeletonMinutia value) {
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
	SkeletonMinutia end() {
		return endMinutia;
	}
	void end(SkeletonMinutia value) {
		if (endMinutia != value) {
			endMinutia = value;
			reversed.start(value);
		}
	}
	void detach() {
		start(null);
		end(null);
	}
	double direction() {
		int first = Parameters.RIDGE_DIRECTION_SKIP;
		int last = Parameters.RIDGE_DIRECTION_SKIP + Parameters.RIDGE_DIRECTION_SAMPLE - 1;
		if (last >= points.size()) {
			int shift = last - points.size() + 1;
			last -= shift;
			first -= shift;
		}
		if (first < 0)
			first = 0;
		return DoubleAngle.atan(points.get(first), points.get(last));
	}
}
