// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

import lombok.*;

public class ScoreShape {
	@Getter private static final double thresholdMaxFMR = 8.48;
	@Getter private static final double thresholdFMR2 = 11.12;
	@Getter private static final double thresholdFMR10 = 14.15;
	@Getter private static final double thresholdFMR100 = 18.22;
	@Getter private static final double thresholdFMR1000 = 22.39;
	@Getter private static final double thresholdFMR10_000 = 27.24;
	@Getter private static final double thresholdFMR100_000 = 32.01;
	public static double shape(double raw) {
		if (raw < thresholdMaxFMR)
			return 0;
		if (raw < thresholdFMR2)
			return interpolate(raw, thresholdMaxFMR, thresholdFMR2, 0, 3);
		if (raw < thresholdFMR10)
			return interpolate(raw, thresholdFMR2, thresholdFMR10, 3, 7);
		if (raw < thresholdFMR100)
			return interpolate(raw, thresholdFMR10, thresholdFMR100, 10, 10);
		if (raw < thresholdFMR1000)
			return interpolate(raw, thresholdFMR100, thresholdFMR1000, 20, 10);
		if (raw < thresholdFMR10_000)
			return interpolate(raw, thresholdFMR1000, thresholdFMR10_000, 30, 10);
		if (raw < thresholdFMR100_000)
			return interpolate(raw, thresholdFMR10_000, thresholdFMR100_000, 40, 10);
		return (raw - thresholdFMR100_000) / (thresholdFMR100_000 - thresholdFMR100) * 30 + 50;
	}
	private static double interpolate(double raw, double min, double max, double start, double length) {
		return (raw - min) / (max - min) * length + start;
	}
}
