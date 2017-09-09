// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

import java.util.function.*;

public class FingerprintContext {
	public static final FingerprintContext defaults = new FingerprintContext();
	private static final ThreadLocal<FingerprintContext> local = new ThreadLocal<>();
	public int blockSize = 15;
	public double dpiTolerance = 10;
	public int histogramDepth = 256;
	public double clippedContrast = 0.08;
	public double minAbsoluteContrast = 17 / 255.0;
	public double minRelativeContrast = 0.34;
	public int relativeContrastSample = 168568;
	public double relativeContrastPercentile = 0.49;
	public VotingParameters contrastVote = new VotingParameters()
		.radius(9)
		.majority(0.86)
		.borderDist(7);
	public VotingParameters maskVote = new VotingParameters()
		.radius(7)
		.majority(0.51)
		.borderDist(4);
	public VotingParameters blockErrorsVote = new VotingParameters()
		.radius(1)
		.majority(0.7)
		.borderDist(4);
	public double maxEqualizationScaling = 3.99;
	public double minEqualizationScaling = 0.25;
	public double minOrientationRadius = 2;
	public double maxOrientationRadius = 6;
	public int orientationSplit = 50;
	public int orientationsChecked = 20;
	public int orientationSmoothingRadius = 1;
	public OrientedLineParams parallelSmoothinig = new OrientedLineParams()
		.resolution(32)
		.radius(7)
		.step(1.59);
	public OrientedLineParams orthogonalSmoothing = new OrientedLineParams()
		.resolution(11)
		.radius(4)
		.step(1.11);
	public VotingParameters binarizedVote = new VotingParameters()
		.radius(2)
		.majority(0.61)
		.borderDist(17);
	public int innerMaskBorderDistance = 14;
	public double maskDisplacement = 10.06;
	public int minutiaCloudRadius = 20;
	public int maxCloudSize = 4;
	public int maxMinutiae = 100;
	public int sortByNeighbor = 5;
	public int edgeTableRange = 490;
	public int edgeTableNeighbors = 9;
	public int thinningIterations = 26;
	public int maxPoreArm = 41;
	public int shortestJoinedEnding = 7;
	public int maxRuptureSize = 5;
	public int maxGapSize = 20;
	public int gapAngleOffset = 22;
	public int toleratedGapOverlap = 2;
	public int minTailLength = 21;
	public int minFragmentLength = 22;
	public int maxDistanceError = 13;
	public double maxAngleError = Math.toRadians(10);
	public double maxGapAngle = Math.toRadians(45);
	public int ridgeDirectionSample = 21;
	public int ridgeDirectionSkip = 1;
	public int maxTriedRoots = 70;
	public int minRootEdgeLength = 58;
	public int maxRootEdgeLookups = 1633;
	public int minSupportingEdges = 1;
	public double distanceErrorFlatness = 0.69;
	public double angleErrorFlatness = 0.27;
	public double pairCountScore = 0.032;
	public double pairFractionScore = 8.98;
	public double correctTypeScore = 0.629;
	public double supportedCountScore = 0.193;
	public double edgeCountScore = 0.265;
	public double distanceAccuracyScore = 9.9;
	public double angleAccuracyScore = 2.79;
	public boolean shapedScore = true;
	public BiConsumer<String, Object> logger;
	public static FingerprintContext current() {
		FingerprintContext custom = local.get();
		return custom != null ? custom : defaults;
	}
	public <T> T get(Supplier<T> supplier) {
		local.set(this);
		try {
			return supplier.get();
		} finally {
			local.set(null);
		}
	}
	public boolean logging() {
		return logger != null;
	}
	public void log(String label, Object data) {
		if (logger != null)
			logger.accept(label, data);
	}
}
