// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis.models;

public class Range {
	public static final Range zero = new Range(0, 0);
	public final int start;
	public final int end;
	public Range(int start, int end) {
		this.start = start;
		this.end = end;
	}
	public int length() {
		return end - start;
	}
}
