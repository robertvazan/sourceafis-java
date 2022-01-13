// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.primitives;

public class IntRange {
	public static final IntRange ZERO = new IntRange(0, 0);
	public final int start;
	public final int end;
	public IntRange(int start, int end) {
		this.start = start;
		this.end = end;
	}
	public int length() {
		return end - start;
	}
}
