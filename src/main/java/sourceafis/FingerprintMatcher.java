package sourceafis;

import java.util.*;
import sourceafis.scalars.*;

public class FingerprintMatcher {
	static final int maxDistanceError = 13;
	static final double maxAngleError = Math.toRadians(10);
	final FingerprintTemplate template;
	Map<Integer, List<IndexedEdge>> edgeHash = new HashMap<>();
	public FingerprintMatcher(FingerprintTemplate template) {
		this.template = template;
		buildEdgeHash();
	}
	static class IndexedEdge {
		final EdgeShape shape;
		final int reference;
		final int neighbor;
		IndexedEdge(EdgeShape shape, int reference, int neighbor) {
			this.shape = shape;
			this.reference = reference;
			this.neighbor = neighbor;
		}
	}
	void buildEdgeHash() {
		for (int referenceMinutia = 0; referenceMinutia < template.minutiae.size(); ++referenceMinutia)
			for (int neighborMinutia = 0; neighborMinutia < template.minutiae.size(); ++neighborMinutia)
				if (referenceMinutia != neighborMinutia) {
					IndexedEdge edge = new IndexedEdge(new EdgeShape(template, referenceMinutia, neighborMinutia), referenceMinutia, neighborMinutia);
					for (int hash : shapeCoverage(edge.shape)) {
						List<IndexedEdge> list = edgeHash.get(hash);
						if (list == null)
							edgeHash.put(hash, list = new ArrayList<>());
						list.add(edge);
					}
				}
	}
	static List<Integer> shapeCoverage(EdgeShape edge) {
		int minLengthBin = (edge.length - maxDistanceError) / maxDistanceError;
		int maxLengthBin = (edge.length + maxDistanceError) / maxDistanceError;
		int angleBins = (int)Math.ceil(2 * Math.PI / maxAngleError);
		int minReferenceBin = (int)(Angle.difference(edge.referenceAngle, maxAngleError) / maxAngleError);
		int maxReferenceBin = (int)(Angle.add(edge.referenceAngle, maxAngleError) / maxAngleError);
		int endReferenceBin = (maxReferenceBin + 1) % angleBins;
		int minNeighborBin = (int)(Angle.difference(edge.neighborAngle, maxAngleError) / maxAngleError);
		int maxNeighborBin = (int)(Angle.add(edge.neighborAngle, maxAngleError) / maxAngleError);
		int endNeighborBin = (maxNeighborBin + 1) % angleBins;
		List<Integer> coverage = new ArrayList<>();
		for (int lengthBin = minLengthBin; lengthBin <= maxLengthBin; ++lengthBin)
			for (int referenceBin = minReferenceBin; referenceBin != endReferenceBin; referenceBin = (referenceBin + 1) % angleBins)
				for (int neighborBin = minNeighborBin; neighborBin != endNeighborBin; neighborBin = (neighborBin + 1) % angleBins)
					coverage.add((referenceBin << 24) + (neighborBin << 16) + lengthBin);
		return coverage;
	}
}
