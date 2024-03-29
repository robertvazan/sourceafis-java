// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.primitives;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

public class IntegersTest {
	@Test
	public void sq() {
		assertEquals(9, Integers.sq(3));
		assertEquals(9, Integers.sq(-3));
	}
	@Test
	public void roundUpDiv() {
		assertEquals(3, Integers.roundUpDiv(9, 3));
		assertEquals(3, Integers.roundUpDiv(8, 3));
		assertEquals(3, Integers.roundUpDiv(7, 3));
		assertEquals(2, Integers.roundUpDiv(6, 3));
		assertEquals(5, Integers.roundUpDiv(20, 4));
		assertEquals(5, Integers.roundUpDiv(19, 4));
		assertEquals(5, Integers.roundUpDiv(18, 4));
		assertEquals(5, Integers.roundUpDiv(17, 4));
		assertEquals(4, Integers.roundUpDiv(16, 4));
	}
}
