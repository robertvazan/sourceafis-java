// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

import lombok.*;

@RequiredArgsConstructor public class MinutiaPair {
	public final int probe;
	public final int candidate;
	@Override public String toString() {
		return String.format("%d<->%d", probe, candidate);
	}
}
