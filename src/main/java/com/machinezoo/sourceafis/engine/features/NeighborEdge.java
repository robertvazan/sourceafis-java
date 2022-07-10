// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.features;

import java.util.*;
import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class NeighborEdge extends EdgeShape {
	public final int neighbor;
	public NeighborEdge(FeatureMinutia[] minutiae, int reference, int neighbor) {
		super(minutiae[reference], minutiae[neighbor]);
		this.neighbor = neighbor;
	}
	public static NeighborEdge[][] buildTable(FeatureMinutia[] minutiae) {
		NeighborEdge[][] edges = new NeighborEdge[minutiae.length][];
		List<NeighborEdge> star = new ArrayList<>();
		int[] allSqDistances = new int[minutiae.length];
		for (int reference = 0; reference < edges.length; ++reference) {
			IntPoint referencePosition = minutiae[reference].position;
			int maxSqDistance = Integer.MAX_VALUE;
			if (minutiae.length - 1 > Parameters.EDGE_TABLE_NEIGHBORS) {
				for (int neighbor = 0; neighbor < minutiae.length; ++neighbor)
					allSqDistances[neighbor] = referencePosition.minus(minutiae[neighbor].position).lengthSq();
				Arrays.sort(allSqDistances);
				maxSqDistance = allSqDistances[Parameters.EDGE_TABLE_NEIGHBORS];
			}
			for (int neighbor = 0; neighbor < minutiae.length; ++neighbor) {
				if (neighbor != reference && referencePosition.minus(minutiae[neighbor].position).lengthSq() <= maxSqDistance)
					star.add(new NeighborEdge(minutiae, reference, neighbor));
			}
			star.sort(Comparator.<NeighborEdge>comparingInt(e -> e.length).thenComparingInt(e -> e.neighbor));
			while (star.size() > Parameters.EDGE_TABLE_NEIGHBORS)
				star.remove(star.size() - 1);
			edges[reference] = star.toArray(new NeighborEdge[star.size()]);
			star.clear();
		}
		// https://sourceafis.machinezoo.com/transparency/edge-table
		TransparencySink.current().log("edge-table", edges);
		return edges;
	}
}
