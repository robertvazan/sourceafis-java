// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

enum MinutiaType {
	ENDING("ending"), BIFURCATION("bifurcation");
	final String json;
	MinutiaType(String json) {
		this.json = json;
	}
}
