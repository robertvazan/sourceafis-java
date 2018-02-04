// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import lombok.*;

@RequiredArgsConstructor enum SkeletonType {
	RIDGES("ridges-"), VALLEYS("valleys-");
	final String prefix;
}
