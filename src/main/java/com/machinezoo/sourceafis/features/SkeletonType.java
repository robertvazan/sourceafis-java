// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.features;

public enum SkeletonType {
	RIDGES("ridges-"), VALLEYS("valleys-");
	public final String prefix;
	SkeletonType(String prefix) {
		this.prefix = prefix;
	}
}
