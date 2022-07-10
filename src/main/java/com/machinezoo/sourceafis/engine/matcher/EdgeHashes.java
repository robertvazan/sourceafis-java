// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

import java.util.*;
import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.templates.*;
import com.machinezoo.sourceafis.engine.transparency.*;
import it.unimi.dsi.fastutil.ints.*;

public class EdgeHashes {
	public static int hash(EdgeShape edge) {
		int lengthBin = edge.length / Parameters.MAX_DISTANCE_ERROR;
		int referenceAngleBin = (int)(edge.referenceAngle / Parameters.MAX_ANGLE_ERROR);
		int neighborAngleBin = (int)(edge.neighborAngle / Parameters.MAX_ANGLE_ERROR);
		return (referenceAngleBin << 24) + (neighborAngleBin << 16) + lengthBin;
	}
	public static boolean matching(EdgeShape probe, EdgeShape candidate) {
		int lengthDelta = probe.length - candidate.length;
		if (lengthDelta >= -Parameters.MAX_DISTANCE_ERROR && lengthDelta <= Parameters.MAX_DISTANCE_ERROR) {
			double complementaryAngleError = DoubleAngle.complementary(Parameters.MAX_ANGLE_ERROR);
			double referenceDelta = DoubleAngle.difference(probe.referenceAngle, candidate.referenceAngle);
			if (referenceDelta <= Parameters.MAX_ANGLE_ERROR || referenceDelta >= complementaryAngleError) {
				double neighborDelta = DoubleAngle.difference(probe.neighborAngle, candidate.neighborAngle);
				if (neighborDelta <= Parameters.MAX_ANGLE_ERROR || neighborDelta >= complementaryAngleError)
					return true;
			}
		}
		return false;
	}
	private static List<Integer> coverage(EdgeShape edge) {
		int minLengthBin = (edge.length - Parameters.MAX_DISTANCE_ERROR) / Parameters.MAX_DISTANCE_ERROR;
		int maxLengthBin = (edge.length + Parameters.MAX_DISTANCE_ERROR) / Parameters.MAX_DISTANCE_ERROR;
		int angleBins = (int)Math.ceil(2 * Math.PI / Parameters.MAX_ANGLE_ERROR);
		int minReferenceBin = (int)(DoubleAngle.difference(edge.referenceAngle, Parameters.MAX_ANGLE_ERROR) / Parameters.MAX_ANGLE_ERROR);
		int maxReferenceBin = (int)(DoubleAngle.add(edge.referenceAngle, Parameters.MAX_ANGLE_ERROR) / Parameters.MAX_ANGLE_ERROR);
		int endReferenceBin = (maxReferenceBin + 1) % angleBins;
		int minNeighborBin = (int)(DoubleAngle.difference(edge.neighborAngle, Parameters.MAX_ANGLE_ERROR) / Parameters.MAX_ANGLE_ERROR);
		int maxNeighborBin = (int)(DoubleAngle.add(edge.neighborAngle, Parameters.MAX_ANGLE_ERROR) / Parameters.MAX_ANGLE_ERROR);
		int endNeighborBin = (maxNeighborBin + 1) % angleBins;
		List<Integer> coverage = new ArrayList<>();
		for (int lengthBin = minLengthBin; lengthBin <= maxLengthBin; ++lengthBin)
			for (int referenceBin = minReferenceBin; referenceBin != endReferenceBin; referenceBin = (referenceBin + 1) % angleBins)
				for (int neighborBin = minNeighborBin; neighborBin != endNeighborBin; neighborBin = (neighborBin + 1) % angleBins)
					coverage.add((referenceBin << 24) + (neighborBin << 16) + lengthBin);
		return coverage;
	}
	public static Int2ObjectMap<List<IndexedEdge>> build(SearchTemplate template) {
		Int2ObjectMap<List<IndexedEdge>> map = new Int2ObjectOpenHashMap<>();
		for (int reference = 0; reference < template.minutiae.length; ++reference)
			for (int neighbor = 0; neighbor < template.minutiae.length; ++neighbor)
				if (reference != neighbor) {
					IndexedEdge edge = new IndexedEdge(template.minutiae, reference, neighbor);
					for (int hash : coverage(edge)) {
						List<IndexedEdge> list = map.get(hash);
						if (list == null)
							map.put(hash, list = new ArrayList<>());
						list.add(edge);
					}
				}
		// https://sourceafis.machinezoo.com/transparency/edge-hash
		TransparencySink.current().logEdgeHash(map);
		return map;
	}
}
