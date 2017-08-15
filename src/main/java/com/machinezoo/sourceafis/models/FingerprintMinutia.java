// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class FingerprintMinutia {
	public final Cell position;
	public final double direction;
	public final MinutiaType type;
	public FingerprintMinutia(Cell position, double direction, MinutiaType type) {
		this.position = position;
		this.direction = direction;
		this.type = type;
	}
}
