// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

class Minutia {
	final Cell position;
	final double direction;
	final MinutiaType type;
	Minutia(Cell position, double direction, MinutiaType type) {
		this.position = position;
		this.direction = direction;
		this.type = type;
	}
	Minutia(JsonMinutia json) {
		position = new Cell(json.x, json.y);
		direction = json.direction;
		type = MinutiaType.BIFURCATION.json.equals(json.type) ? MinutiaType.BIFURCATION : MinutiaType.ENDING;
	}
	@Override public String toString() {
		return String.format("%s @ %s angle %f", type.toString(), position.toString(), direction);
	}
}
