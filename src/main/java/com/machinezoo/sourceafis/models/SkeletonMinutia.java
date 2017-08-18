// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

import java.util.*;

public class SkeletonMinutia {
	public boolean considered = true;
	public final Cell position;
	public final List<SkeletonRidge> ridges = new ArrayList<>();
	public SkeletonMinutia(Cell position) {
		this.position = position;
	}
	public void attachStart(SkeletonRidge ridge) {
		if (!ridges.contains(ridge)) {
			ridges.add(ridge);
			ridge.start(this);
		}
	}
	public void detachStart(SkeletonRidge ridge) {
		if (ridges.contains(ridge)) {
			ridges.remove(ridge);
			if (ridge.start() == this)
				ridge.start(null);
		}
	}
	@Override public String toString() {
		return String.format("%s*%d%s", position.toString(), ridges.size(), considered ? "" : "(ignored)");
	}
}
