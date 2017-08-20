package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import org.junit.*;

public class FingerprintMatcherTest {
	@Test public void matchingPair() {
		FingerprintMatcher matcher = new FingerprintMatcher(FingerprintTemplateTest.probe());
		double score = matcher.match(FingerprintTemplateTest.matching());
		assertTrue("Score: " + score, score > 40);
	}
	@Test public void nonmatchingPair() {
		FingerprintMatcher matcher = new FingerprintMatcher(FingerprintTemplateTest.probe());
		double score = matcher.match(FingerprintTemplateTest.nonmatching());
		assertTrue("Score: " + score, score < 20);
	}
}
