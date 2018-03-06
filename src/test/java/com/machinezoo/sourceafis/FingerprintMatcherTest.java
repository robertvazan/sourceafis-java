// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import org.junit.*;

public class FingerprintMatcherTest {
	@Test public void matchingPair() {
		FingerprintMatcher matcher = new FingerprintMatcher().index(FingerprintTemplateTest.probe());
		double score = matcher.match(FingerprintTemplateTest.matching());
		assertTrue("Score: " + score, score > 40);
	}
	@Test public void nonmatchingPair() {
		FingerprintMatcher matcher = new FingerprintMatcher().index(FingerprintTemplateTest.probe());
		double score = matcher.match(FingerprintTemplateTest.nonmatching());
		assertTrue("Score: " + score, score < 20);
	}
	@Test public void matchingPairIso() {
		FingerprintMatcher matcher = new FingerprintMatcher().index(FingerprintTemplateTest.probeIso());
		double score = matcher.match(FingerprintTemplateTest.matchingIso());
		assertTrue("Score: " + score, score > 40);
	}
	@Test public void nonmatchingPairIso() {
		FingerprintMatcher matcher = new FingerprintMatcher().index(FingerprintTemplateTest.probeIso());
		double score = matcher.match(FingerprintTemplateTest.nonmatchingIso());
		assertTrue("Score: " + score, score < 20);
	}
}
