// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import org.junit.jupiter.api.*;

public class FingerprintMatcherTest {
	private void matching(FingerprintTemplate probe, FingerprintTemplate candidate) {
		double score = new FingerprintMatcher(probe)
			.match(candidate);
		assertThat(score, greaterThan(40.0));
	}
	private void nonmatching(FingerprintTemplate probe, FingerprintTemplate candidate) {
		double score = new FingerprintMatcher(probe)
			.match(candidate);
		assertThat(score, lessThan(20.0));
	}
	@Test
	public void matchingPair() {
		matching(FingerprintTemplateTest.probe(), FingerprintTemplateTest.matching());
	}
	@Test
	public void nonmatchingPair() {
		nonmatching(FingerprintTemplateTest.probe(), FingerprintTemplateTest.nonmatching());
	}
	@Test
	public void matchingIso() {
		matching(FingerprintCompatibilityTest.probeIso(), FingerprintCompatibilityTest.matchingIso());
	}
	@Test
	public void nonmatchingIso() {
		nonmatching(FingerprintCompatibilityTest.probeIso(), FingerprintCompatibilityTest.nonmatchingIso());
	}
	@Test
	public void matchingGray() {
		matching(FingerprintTemplateTest.probeGray(), FingerprintTemplateTest.matchingGray());
	}
	@Test
	public void nonmatchingGray() {
		nonmatching(FingerprintTemplateTest.probeGray(), FingerprintTemplateTest.nonmatchingGray());
	}
}
