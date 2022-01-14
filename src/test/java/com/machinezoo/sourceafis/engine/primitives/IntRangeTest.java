// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.primitives;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

public class IntRangeTest {
	@Test
	public void constructor() {
		IntRange r = new IntRange(3, 10);
		assertEquals(3, r.start);
		assertEquals(10, r.end);
	}
	@Test
	public void length() {
		assertEquals(7, new IntRange(3, 10).length());
	}
}
