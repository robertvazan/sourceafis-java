// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.templates;

import com.machinezoo.sourceafis.engine.primitives.*;

class TemplateResolution {
	double dpiX;
	double dpiY;
	IntPoint decode(int x, int y) {
		return new IntPoint(DpiConverter.decode(x, dpiX), DpiConverter.decode(y, dpiY));
	}
}
