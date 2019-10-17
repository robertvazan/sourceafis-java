// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

enum MinutiaType {
	ENDING("ending"), BIFURCATION("bifurcation");
	final String json;
	MinutiaType(String json) {
		this.json = json;
	}
}
