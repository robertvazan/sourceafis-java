// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

class Score {
	int minutiaCount;
	double minutiaScore;
	double minutiaFractionInProbe;
	double minutiaFractionInCandidate;
	double minutiaFraction;
	double minutiaFractionScore;
	int supportingEdgeSum;
	int edgeCount;
	double edgeScore;
	int supportedMinutiaCount;
	double supportedMinutiaScore;
	int minutiaTypeHits;
	double minutiaTypeScore;
	int distanceErrorSum;
	int distanceAccuracySum;
	double distanceAccuracyScore;
	double angleErrorSum;
	double angleAccuracySum;
	double angleAccuracyScore;
	double totalScore;
	double shapedScore;
	void compute(MatcherThread thread) {
		minutiaCount = thread.count;
		minutiaScore = Parameters.minutiaCountScore * minutiaCount;
		minutiaFractionInProbe = thread.count / (double)thread.probe.minutiae.length;
		minutiaFractionInCandidate = thread.count / (double)thread.candidate.minutiae.length;
		minutiaFraction = 0.5 * (minutiaFractionInProbe + minutiaFractionInCandidate);
		minutiaFractionScore = Parameters.minutiaFractionScore * minutiaFraction;
		supportingEdgeSum = 0;
		supportedMinutiaCount = 0;
		minutiaTypeHits = 0;
		for (int i = 0; i < thread.count; ++i) {
			MinutiaPair pair = thread.tree[i];
			supportingEdgeSum += pair.supportingEdges;
			if (pair.supportingEdges >= Parameters.minSupportingEdges)
				++supportedMinutiaCount;
			if (thread.probe.minutiae[pair.probe].type == thread.candidate.minutiae[pair.candidate].type)
				++minutiaTypeHits;
		}
		edgeCount = thread.count + supportingEdgeSum;
		edgeScore = Parameters.edgeScore * edgeCount;
		supportedMinutiaScore = Parameters.supportedMinutiaScore * supportedMinutiaCount;
		minutiaTypeScore = Parameters.minutiaTypeScore * minutiaTypeHits;
		int innerDistanceRadius = (int)Math.round(Parameters.distanceErrorFlatness * Parameters.maxDistanceError);
		double innerAngleRadius = Parameters.angleErrorFlatness * Parameters.maxAngleError;
		distanceErrorSum = 0;
		angleErrorSum = 0;
		for (int i = 1; i < thread.count; ++i) {
			MinutiaPair pair = thread.tree[i];
			EdgeShape probeEdge = new EdgeShape(thread.probe.minutiae[pair.probeRef], thread.probe.minutiae[pair.probe]);
			EdgeShape candidateEdge = new EdgeShape(thread.candidate.minutiae[pair.candidateRef], thread.candidate.minutiae[pair.candidate]);
			distanceErrorSum += Math.max(innerDistanceRadius, Math.abs(probeEdge.length - candidateEdge.length));
			angleErrorSum += Math.max(innerAngleRadius, DoubleAngle.distance(probeEdge.referenceAngle, candidateEdge.referenceAngle));
			angleErrorSum += Math.max(innerAngleRadius, DoubleAngle.distance(probeEdge.neighborAngle, candidateEdge.neighborAngle));
		}
		distanceAccuracyScore = 0;
		angleAccuracyScore = 0;
		int distanceErrorPotential = Parameters.maxDistanceError * Math.max(0, thread.count - 1);
		distanceAccuracySum = distanceErrorPotential - distanceErrorSum;
		distanceAccuracyScore = Parameters.distanceAccuracyScore * (distanceErrorPotential > 0 ? distanceAccuracySum / (double)distanceErrorPotential : 0);
		double angleErrorPotential = Parameters.maxAngleError * Math.max(0, thread.count - 1) * 2;
		angleAccuracySum = angleErrorPotential - angleErrorSum;
		angleAccuracyScore = Parameters.angleAccuracyScore * (angleErrorPotential > 0 ? angleAccuracySum / angleErrorPotential : 0);
		totalScore = minutiaScore
			+ minutiaFractionScore
			+ supportedMinutiaScore
			+ edgeScore
			+ minutiaTypeScore
			+ distanceAccuracyScore
			+ angleAccuracyScore;
		shapedScore = shape(totalScore);
	}
	private static double shape(double raw) {
		if (raw < Parameters.thresholdMaxFMR)
			return 0;
		if (raw < Parameters.thresholdFMR2)
			return interpolate(raw, Parameters.thresholdMaxFMR, Parameters.thresholdFMR2, 0, 3);
		if (raw < Parameters.thresholdFMR10)
			return interpolate(raw, Parameters.thresholdFMR2, Parameters.thresholdFMR10, 3, 7);
		if (raw < Parameters.thresholdFMR100)
			return interpolate(raw, Parameters.thresholdFMR10, Parameters.thresholdFMR100, 10, 10);
		if (raw < Parameters.thresholdFMR1000)
			return interpolate(raw, Parameters.thresholdFMR100, Parameters.thresholdFMR1000, 20, 10);
		if (raw < Parameters.thresholdFMR10_000)
			return interpolate(raw, Parameters.thresholdFMR1000, Parameters.thresholdFMR10_000, 30, 10);
		if (raw < Parameters.thresholdFMR100_000)
			return interpolate(raw, Parameters.thresholdFMR10_000, Parameters.thresholdFMR100_000, 40, 10);
		return (raw - Parameters.thresholdFMR100_000) / (Parameters.thresholdFMR100_000 - Parameters.thresholdFMR100) * 30 + 50;
	}
	private static double interpolate(double raw, double min, double max, double start, double length) {
		return (raw - min) / (max - min) * length + start;
	}
}
