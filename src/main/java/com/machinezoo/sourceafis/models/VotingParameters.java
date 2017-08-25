// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

import lombok.*;

public class VotingParameters {
	@Setter public int radius = 1;
	@Setter public double majority = 0.5;
	@Setter public int borderDist = 0;
}
