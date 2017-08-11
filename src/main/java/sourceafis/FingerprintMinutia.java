// Part of SourceAFIS: https://sourceafis.machinezoo.com
package sourceafis;

import sourceafis.scalars.*;

class FingerprintMinutia {
	final Cell position;
	final double direction;
	final MinutiaType type;
	public FingerprintMinutia(Cell position, double direction, MinutiaType type) {
		this.position = position;
		this.direction = direction;
		this.type = type;
	}
}
