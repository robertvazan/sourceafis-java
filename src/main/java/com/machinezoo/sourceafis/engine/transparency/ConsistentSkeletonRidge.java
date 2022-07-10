// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.transparency;

import java.util.*;
import com.machinezoo.sourceafis.engine.primitives.*;

public class ConsistentSkeletonRidge {
	public final int start;
	public final int end;
	public final List<IntPoint> points;
	public ConsistentSkeletonRidge(int start, int end, List<IntPoint> points) {
		this.start = start;
		this.end = end;
		this.points = points;
	}
}
