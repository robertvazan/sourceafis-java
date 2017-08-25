// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

import lombok.*;

@AllArgsConstructor public class IndexedEdge {
	public final EdgeShape shape;
	public final int reference;
	public final int neighbor;
}
