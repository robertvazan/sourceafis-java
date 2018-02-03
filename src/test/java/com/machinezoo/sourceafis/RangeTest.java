// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import org.junit.*;

public class RangeTest {
	@Test public void constructor() {
		Range r = new Range(3, 10);
		assertEquals(3, r.start);
		assertEquals(10, r.end);
	}
	@Test public void length() {
		assertEquals(7, new Range(3, 10).length());
	}
}
