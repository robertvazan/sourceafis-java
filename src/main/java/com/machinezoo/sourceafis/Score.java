// Part of SourceAFIS: https://sourceafis.machinezoo.com
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
	void compute(MatchBuffer match) {
		matchedMinutiae = match.count;
		matchedMinutiaeScore = Parameters.pairCountScore * matchedMinutiae;
		matchedFractionOfProbeMinutiae = match.count / (double)match.probe.minutiae.length;
		matchedFractionOfCandidateMinutiae = match.count / (double)match.candidate.minutiae.length;
		matchedFractionOfAllMinutiaeScore = Parameters.pairFractionScore * (matchedFractionOfProbeMinutiae + matchedFractionOfCandidateMinutiae) / 2;
		matchedEdges = match.count;
		minutiaeWithSeveralEdges = 0;
		correctMinutiaTypeCount = 0;
		for (int i = 0; i < match.count; ++i) {
			MinutiaPair pair = match.tree[i];
			matchedEdges += pair.supportingEdges;
			if (pair.supportingEdges >= Parameters.minSupportingEdges)
				++minutiaeWithSeveralEdges;
			if (match.probe.minutiae[pair.probe].type == match.candidate.minutiae[pair.candidate].type)
				++correctMinutiaTypeCount;
		}
		matchedEdgesScore = Parameters.edgeCountScore * matchedEdges;
		minutiaeWithSeveralEdgesScore = Parameters.supportedCountScore * minutiaeWithSeveralEdges;
		correctMinutiaTypeScore = Parameters.correctTypeScore * correctMinutiaTypeCount;
		int innerDistanceRadius = (int)Math.round(Parameters.distanceErrorFlatness * Parameters.maxDistanceError);
		int innerAngleRadius = (int)Math.round(Parameters.angleErrorFlatness * Parameters.maxAngleError);
		int distanceErrorSum = 0;
		int angleErrorSum = 0;
		for (int i = 1; i < match.count; ++i) {
			MinutiaPair pair = match.tree[i];
			EdgeShape probeEdge = new EdgeShape(match.probe.minutiae[pair.probeRef], match.probe.minutiae[pair.probe]);
			EdgeShape candidateEdge = new EdgeShape(match.candidate.minutiae[pair.candidateRef], match.candidate.minutiae[pair.candidate]);
			distanceErrorSum += Math.max(innerDistanceRadius, Math.abs(probeEdge.length - candidateEdge.length));
			angleErrorSum += Math.max(innerAngleRadius, Angle.distance(probeEdge.referenceAngle, candidateEdge.referenceAngle));
			angleErrorSum += Math.max(innerAngleRadius, Angle.distance(probeEdge.neighborAngle, candidateEdge.neighborAngle));
		}
		accurateEdgeLengthScore = 0;
		accurateMinutiaAngleScore = 0;
		if (match.count >= 2) {
			double pairedDistanceError = Parameters.maxDistanceError * (match.count - 1);
			accurateEdgeLengthScore = Parameters.distanceAccuracyScore * (pairedDistanceError - distanceErrorSum) / pairedDistanceError;
			double pairedAngleError = Parameters.maxAngleError * (match.count - 1) * 2;
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
