// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.features;

import java.util.*;
import com.machinezoo.sourceafis.configuration.*;
import com.machinezoo.sourceafis.primitives.*;

public class SkeletonRidge {
	public final List<IntPoint> points;
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
	public double direction() {
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
