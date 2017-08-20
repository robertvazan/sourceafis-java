package com.machinezoo.sourceafis.models;

import lombok.*;

@NoArgsConstructor @AllArgsConstructor public class PairInfo {
	public MinutiaPair pair;
	public MinutiaPair reference;
	public int supportingEdges;
	@Override public String toString() {
		return String.format("%s -> %s #%d", reference, pair, supportingEdges);
	}
}
