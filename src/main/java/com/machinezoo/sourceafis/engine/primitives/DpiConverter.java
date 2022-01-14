// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.primitives;

public class DpiConverter {
	public static int decode(int value, double dpi) {
		return (int)Math.round(value / dpi * 500);
	}
}
