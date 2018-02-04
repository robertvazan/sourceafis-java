// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import lombok.*;

@RequiredArgsConstructor enum MinutiaType {
	ENDING("ending"), BIFURCATION("bifurcation");
	final String json;
}
