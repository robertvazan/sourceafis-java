// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.transparency;

import com.machinezoo.sourceafis.*;

/*
 * To avoid null checks everywhere, we have one noop transparency logger as a fallback.
 */
public class NoTransparency extends FingerprintTransparency {
	public static final TransparencySink SINK;
	static {
		try (var transparency = new NoTransparency()) {
			SINK = TransparencySink.current();
		}
	}
	@Override
	public boolean accepts(String key) {
		return false;
	}
}
