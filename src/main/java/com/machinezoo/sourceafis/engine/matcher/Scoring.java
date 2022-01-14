// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.matcher;

import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.templates.*;

public class Scoring {
	public int minutiaCount;
	public double minutiaScore;
	public double minutiaFractionInProbe;
	public double minutiaFractionInCandidate;
	public double minutiaFraction;
	public double minutiaFractionScore;
	public int supportingEdgeSum;
	public int edgeCount;
	public double edgeScore;
	public int supportedMinutiaCount;
	public double supportedMinutiaScore;
	public int minutiaTypeHits;
	public double minutiaTypeScore;
	public int distanceErrorSum;
	public int distanceAccuracySum;
	public double distanceAccuracyScore;
	public double angleErrorSum;
	public double angleAccuracySum;
	public double angleAccuracyScore;
	public double totalScore;
	public double shapedScore;
	public void compute(ImmutableProbe probe, ImmutableTemplate candidate, PairingGraph pairing) {
		var pminutiae = probe.template.minutiae;
		var cminutiae = candidate.minutiae;
		minutiaCount = pairing.count;
		minutiaScore = Parameters.MINUTIA_SCORE * minutiaCount;
		minutiaFractionInProbe = pairing.count / (double)pminutiae.length;
		minutiaFractionInCandidate = pairing.count / (double)cminutiae.length;
		minutiaFraction = 0.5 * (minutiaFractionInProbe + minutiaFractionInCandidate);
		minutiaFractionScore = Parameters.MINUTIA_FRACTION_SCORE * minutiaFraction;
		supportingEdgeSum = 0;
		supportedMinutiaCount = 0;
		minutiaTypeHits = 0;
		for (int i = 0; i < pairing.count; ++i) {
			MinutiaPair pair = pairing.tree[i];
			supportingEdgeSum += pair.supportingEdges;
			if (pair.supportingEdges >= Parameters.MIN_SUPPORTING_EDGES)
				++supportedMinutiaCount;
			if (pminutiae[pair.probe].type == cminutiae[pair.candidate].type)
				++minutiaTypeHits;
		}
		edgeCount = pairing.count + supportingEdgeSum;
		edgeScore = Parameters.EDGE_SCORE * edgeCount;
		supportedMinutiaScore = Parameters.SUPPORTED_MINUTIA_SCORE * supportedMinutiaCount;
		minutiaTypeScore = Parameters.MINUTIA_TYPE_SCORE * minutiaTypeHits;
		int innerDistanceRadius = (int)Math.round(Parameters.DISTANCE_ERROR_FLATNESS * Parameters.MAX_DISTANCE_ERROR);
		double innerAngleRadius = Parameters.ANGLE_ERROR_FLATNESS * Parameters.MAX_ANGLE_ERROR;
		distanceErrorSum = 0;
		angleErrorSum = 0;
		for (int i = 1; i < pairing.count; ++i) {
			MinutiaPair pair = pairing.tree[i];
			EdgeShape probeEdge = new EdgeShape(pminutiae[pair.probeRef], pminutiae[pair.probe]);
			EdgeShape candidateEdge = new EdgeShape(cminutiae[pair.candidateRef], cminutiae[pair.candidate]);
			distanceErrorSum += Math.max(innerDistanceRadius, Math.abs(probeEdge.length - candidateEdge.length));
			angleErrorSum += Math.max(innerAngleRadius, DoubleAngle.distance(probeEdge.referenceAngle, candidateEdge.referenceAngle));
			angleErrorSum += Math.max(innerAngleRadius, DoubleAngle.distance(probeEdge.neighborAngle, candidateEdge.neighborAngle));
		}
		distanceAccuracyScore = 0;
		angleAccuracyScore = 0;
		int distanceErrorPotential = Parameters.MAX_DISTANCE_ERROR * Math.max(0, pairing.count - 1);
		distanceAccuracySum = distanceErrorPotential - distanceErrorSum;
		distanceAccuracyScore = Parameters.DISTANCE_ACCURACY_SCORE * (distanceErrorPotential > 0 ? distanceAccuracySum / (double)distanceErrorPotential : 0);
		double angleErrorPotential = Parameters.MAX_ANGLE_ERROR * Math.max(0, pairing.count - 1) * 2;
		angleAccuracySum = angleErrorPotential - angleErrorSum;
		angleAccuracyScore = Parameters.ANGLE_ACCURACY_SCORE * (angleErrorPotential > 0 ? angleAccuracySum / angleErrorPotential : 0);
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
