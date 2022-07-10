// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.templates.*;

public class Scoring {
	public static void compute(SearchTemplate probe, SearchTemplate candidate, PairingGraph pairing, ScoringData score) {
		var pminutiae = probe.minutiae;
		var cminutiae = candidate.minutiae;
		score.minutiaCount = pairing.count;
		score.minutiaScore = Parameters.MINUTIA_SCORE * score.minutiaCount;
		score.minutiaFractionInProbe = pairing.count / (double)pminutiae.length;
		score.minutiaFractionInCandidate = pairing.count / (double)cminutiae.length;
		score.minutiaFraction = 0.5 * (score.minutiaFractionInProbe + score.minutiaFractionInCandidate);
		score.minutiaFractionScore = Parameters.MINUTIA_FRACTION_SCORE * score.minutiaFraction;
		score.supportingEdgeSum = 0;
		score.supportedMinutiaCount = 0;
		score.minutiaTypeHits = 0;
		for (int i = 0; i < pairing.count; ++i) {
			MinutiaPair pair = pairing.tree[i];
			score.supportingEdgeSum += pair.supportingEdges;
			if (pair.supportingEdges >= Parameters.MIN_SUPPORTING_EDGES)
				++score.supportedMinutiaCount;
			if (pminutiae[pair.probe].type == cminutiae[pair.candidate].type)
				++score.minutiaTypeHits;
		}
		score.edgeCount = pairing.count + score.supportingEdgeSum;
		score.edgeScore = Parameters.EDGE_SCORE * score.edgeCount;
		score.supportedMinutiaScore = Parameters.SUPPORTED_MINUTIA_SCORE * score.supportedMinutiaCount;
		score.minutiaTypeScore = Parameters.MINUTIA_TYPE_SCORE * score.minutiaTypeHits;
		int innerDistanceRadius = (int)Math.round(Parameters.DISTANCE_ERROR_FLATNESS * Parameters.MAX_DISTANCE_ERROR);
		float innerAngleRadius = (float)(Parameters.ANGLE_ERROR_FLATNESS * Parameters.MAX_ANGLE_ERROR);
		score.distanceErrorSum = 0;
		score.angleErrorSum = 0;
		for (int i = 1; i < pairing.count; ++i) {
			MinutiaPair pair = pairing.tree[i];
			EdgeShape probeEdge = new EdgeShape(pminutiae[pair.probeRef], pminutiae[pair.probe]);
			EdgeShape candidateEdge = new EdgeShape(cminutiae[pair.candidateRef], cminutiae[pair.candidate]);
			score.distanceErrorSum += Math.max(innerDistanceRadius, Math.abs(probeEdge.length - candidateEdge.length));
			score.angleErrorSum += Math.max(innerAngleRadius, FloatAngle.distance(probeEdge.referenceAngle, candidateEdge.referenceAngle));
			score.angleErrorSum += Math.max(innerAngleRadius, FloatAngle.distance(probeEdge.neighborAngle, candidateEdge.neighborAngle));
		}
		score.distanceAccuracyScore = 0;
		score.angleAccuracyScore = 0;
		int distanceErrorPotential = Parameters.MAX_DISTANCE_ERROR * Math.max(0, pairing.count - 1);
		score.distanceAccuracySum = distanceErrorPotential - score.distanceErrorSum;
		score.distanceAccuracyScore = Parameters.DISTANCE_ACCURACY_SCORE * (distanceErrorPotential > 0 ? score.distanceAccuracySum / (double)distanceErrorPotential : 0);
		float angleErrorPotential = Parameters.MAX_ANGLE_ERROR * Math.max(0, pairing.count - 1) * 2;
		score.angleAccuracySum = angleErrorPotential - score.angleErrorSum;
		score.angleAccuracyScore = Parameters.ANGLE_ACCURACY_SCORE * (angleErrorPotential > 0 ? score.angleAccuracySum / angleErrorPotential : 0);
		score.totalScore = score.minutiaScore
			+ score.minutiaFractionScore
			+ score.supportedMinutiaScore
			+ score.edgeScore
			+ score.minutiaTypeScore
			+ score.distanceAccuracyScore
			+ score.angleAccuracyScore;
		score.shapedScore = shape(score.totalScore);
	}
	private static double shape(double raw) {
		if (raw < Parameters.THRESHOLD_FMR_MAX)
			return 0;
		if (raw < Parameters.THRESHOLD_FMR_2)
			return interpolate(raw, Parameters.THRESHOLD_FMR_MAX, Parameters.THRESHOLD_FMR_2, 0, 3);
		if (raw < Parameters.THRESHOLD_FMR_10)
			return interpolate(raw, Parameters.THRESHOLD_FMR_2, Parameters.THRESHOLD_FMR_10, 3, 7);
		if (raw < Parameters.THRESHOLD_FMR_100)
			return interpolate(raw, Parameters.THRESHOLD_FMR_10, Parameters.THRESHOLD_FMR_100, 10, 10);
		if (raw < Parameters.THRESHOLD_FMR_1000)
			return interpolate(raw, Parameters.THRESHOLD_FMR_100, Parameters.THRESHOLD_FMR_1000, 20, 10);
		if (raw < Parameters.THRESHOLD_FMR_10_000)
			return interpolate(raw, Parameters.THRESHOLD_FMR_1000, Parameters.THRESHOLD_FMR_10_000, 30, 10);
		if (raw < Parameters.THRESHOLD_FMR_100_000)
			return interpolate(raw, Parameters.THRESHOLD_FMR_10_000, Parameters.THRESHOLD_FMR_100_000, 40, 10);
		return (raw - Parameters.THRESHOLD_FMR_100_000) / (Parameters.THRESHOLD_FMR_100_000 - Parameters.THRESHOLD_FMR_100) * 30 + 50;
	}
	private static double interpolate(double raw, double min, double max, double start, double length) {
		return (raw - min) / (max - min) * length + start;
	}
}
