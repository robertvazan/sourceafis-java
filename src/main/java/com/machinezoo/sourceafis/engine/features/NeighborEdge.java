// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.features;

import java.util.*;
import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.transparency.*;

public class NeighborEdge extends EdgeShape {
    public final short neighbor;
    public NeighborEdge(SearchMinutia[] minutiae, int reference, int neighbor) {
        super(minutiae[reference], minutiae[neighbor]);
        this.neighbor = (short)neighbor;
    }
    public static NeighborEdge[][] buildTable(SearchMinutia[] minutiae) {
        NeighborEdge[][] edges = new NeighborEdge[minutiae.length][];
        List<NeighborEdge> star = new ArrayList<>();
        int[] allSqDistances = new int[minutiae.length];
        for (int reference = 0; reference < edges.length; ++reference) {
            var rminutia = minutiae[reference];
            int maxSqDistance = Integer.MAX_VALUE;
            if (minutiae.length - 1 > Parameters.EDGE_TABLE_NEIGHBORS) {
                for (int neighbor = 0; neighbor < minutiae.length; ++neighbor) {
                    var nminutia = minutiae[neighbor];
                    allSqDistances[neighbor] = Integers.sq(rminutia.x - nminutia.x) + Integers.sq(rminutia.y - nminutia.y);
                }
                Arrays.sort(allSqDistances);
                maxSqDistance = allSqDistances[Parameters.EDGE_TABLE_NEIGHBORS];
            }
            for (int neighbor = 0; neighbor < minutiae.length; ++neighbor) {
                var nminutia = minutiae[neighbor];
                if (neighbor != reference && Integers.sq(rminutia.x - nminutia.x) + Integers.sq(rminutia.y - nminutia.y) <= maxSqDistance)
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
    public static int memory() { return MemoryEstimates.object(2 * Short.BYTES + 2 * Float.BYTES, Float.BYTES); }
}
