// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

class Score {
	int matchedMinutiae;
	double matchedMinutiaeScore;
	double matchedFractionOfProbeMinutiae;
	double matchedFractionOfCandidateMinutiae;
	double matchedFractionOfAllMinutiaeScore;
	int matchedEdges;
	double matchedEdgesScore;
	int minutiaeWithSeveralEdges;
	double minutiaeWithSeveralEdgesScore;
	int correctMinutiaTypeCount;
	double correctMinutiaTypeScore;
	double accurateEdgeLengthScore;
	double accurateMinutiaAngleScore;
	double totalScore;
	double shapedScore;
	void compute(MatcherThread thread) {
		matchedMinutiae = thread.count;
		matchedMinutiaeScore = Parameters.pairCountScore * matchedMinutiae;
		matchedFractionOfProbeMinutiae = thread.count / (double)thread.probe.minutiae.length;
		matchedFractionOfCandidateMinutiae = thread.count / (double)thread.candidate.minutiae.length;
		matchedFractionOfAllMinutiaeScore = Parameters.pairFractionScore * (matchedFractionOfProbeMinutiae + matchedFractionOfCandidateMinutiae) / 2;
		matchedEdges = thread.count;
		minutiaeWithSeveralEdges = 0;
		correctMinutiaTypeCount = 0;
		for (int i = 0; i < thread.count; ++i) {
			MinutiaPair pair = thread.tree[i];
			matchedEdges += pair.supportingEdges;
			if (pair.supportingEdges >= Parameters.minSupportingEdges)
				++minutiaeWithSeveralEdges;
			if (thread.probe.minutiae[pair.probe].type == thread.candidate.minutiae[pair.candidate].type)
				++correctMinutiaTypeCount;
		}
		matchedEdgesScore = Parameters.edgeCountScore * matchedEdges;
		minutiaeWithSeveralEdgesScore = Parameters.supportedCountScore * minutiaeWithSeveralEdges;
		correctMinutiaTypeScore = Parameters.correctTypeScore * correctMinutiaTypeCount;
		int innerDistanceRadius = (int)Math.round(Parameters.distanceErrorFlatness * Parameters.maxDistanceError);
		double innerAngleRadius = Parameters.angleErrorFlatness * Parameters.maxAngleError;
		int distanceErrorSum = 0;
		double angleErrorSum = 0;
		for (int i = 1; i < thread.count; ++i) {
			MinutiaPair pair = thread.tree[i];
			EdgeShape probeEdge = new EdgeShape(thread.probe.minutiae[pair.probeRef], thread.probe.minutiae[pair.probe]);
			EdgeShape candidateEdge = new EdgeShape(thread.candidate.minutiae[pair.candidateRef], thread.candidate.minutiae[pair.candidate]);
			distanceErrorSum += Math.max(innerDistanceRadius, Math.abs(probeEdge.length - candidateEdge.length));
			angleErrorSum += Math.max(innerAngleRadius, DoubleAngle.distance(probeEdge.referenceAngle, candidateEdge.referenceAngle));
			angleErrorSum += Math.max(innerAngleRadius, DoubleAngle.distance(probeEdge.neighborAngle, candidateEdge.neighborAngle));
		}
		accurateEdgeLengthScore = 0;
		accurateMinutiaAngleScore = 0;
		if (thread.count >= 2) {
			double pairedDistanceError = Parameters.maxDistanceError * (thread.count - 1);
			accurateEdgeLengthScore = Parameters.distanceAccuracyScore * (pairedDistanceError - distanceErrorSum) / pairedDistanceError;
			double pairedAngleError = Parameters.maxAngleError * (thread.count - 1) * 2;
			accurateMinutiaAngleScore = Parameters.angleAccuracyScore * (pairedAngleError - angleErrorSum) / pairedAngleError;
		}
		totalScore = matchedMinutiaeScore
			+ matchedFractionOfAllMinutiaeScore
			+ minutiaeWithSeveralEdgesScore
			+ matchedEdgesScore
			+ correctMinutiaTypeScore
			+ accurateEdgeLengthScore
			+ accurateMinutiaAngleScore;
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
