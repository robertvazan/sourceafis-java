// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.templates;

import java.util.*;
import com.machinezoo.sourceafis.*;
import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.primitives.*;

public class PersistentTemplate {
	public String version;
	public int width;
	public int height;
	public int[] positionsX;
	public int[] positionsY;
	public double[] directions;
	public String types;
	public PersistentTemplate() {
	}
	public PersistentTemplate(MutableTemplate mutable) {
		version = FingerprintCompatibility.version();
		width = mutable.size.x;
		height = mutable.size.y;
		int count = mutable.minutiae.size();
		positionsX = new int[count];
		positionsY = new int[count];
		directions = new double[count];
		char[] chars = new char[count];
		for (int i = 0; i < count; ++i) {
			MutableMinutia minutia = mutable.minutiae.get(i);
			positionsX[i] = minutia.position.x;
			positionsY[i] = minutia.position.y;
			directions[i] = minutia.direction;
			chars[i] = minutia.type == MinutiaType.BIFURCATION ? 'B' : 'E';
		}
		types = new String(chars);
	}
	public MutableTemplate mutable() {
		MutableTemplate mutable = new MutableTemplate();
		mutable.size = new IntPoint(width, height);
		mutable.minutiae = new ArrayList<>();
		for (int i = 0; i < types.length(); ++i) {
			MinutiaType type = types.charAt(i) == 'B' ? MinutiaType.BIFURCATION : MinutiaType.ENDING;
			mutable.minutiae.add(new MutableMinutia(new IntPoint(positionsX[i], positionsY[i]), directions[i], type));
		}
		return mutable;
	}
	public void validate() {
		/*
		 * Width and height are informative only. Don't validate them. Ditto for version string.
		 */
		Objects.requireNonNull(positionsX, "Null array of X positions.");
		Objects.requireNonNull(positionsY, "Null array of Y positions.");
		Objects.requireNonNull(directions, "Null array of minutia directions.");
		Objects.requireNonNull(types, "Null minutia type string.");
		if (positionsX.length != types.length() || positionsY.length != types.length() || directions.length != types.length())
			throw new IllegalArgumentException("Inconsistent lengths of minutia property arrays.");
		for (int i = 0; i < types.length(); ++i) {
			if (Math.abs(positionsX[i]) > 10_000 || Math.abs(positionsY[i]) > 10_000)
				throw new IllegalArgumentException("Minutia position out of range.");
			if (!DoubleAngle.normalized(directions[i]))
				throw new IllegalArgumentException("Denormalized minutia direction.");
			if (types.charAt(i) != 'E' && types.charAt(i) != 'B')
				throw new IllegalArgumentException("Unknown minutia type.");
		}
	}
}
