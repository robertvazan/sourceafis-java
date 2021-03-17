// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import org.junit.jupiter.api.*;
import com.machinezoo.fingerprintio.*;

public class FingerprintCompatibilityTest {
	@Test
	public void version() {
		assertThat(FingerprintCompatibility.version(), matchesPattern("^\\d+\\.\\d+\\.\\d+$"));
	}
	public static FingerprintTemplate probeIso() {
		return FingerprintCompatibility.importTemplate(TestResources.probeIso());
	}
	public static FingerprintTemplate matchingIso() {
		return FingerprintCompatibility.importTemplate(TestResources.matchingIso());
	}
	public static FingerprintTemplate nonmatchingIso() {
		return FingerprintCompatibility.importTemplate(TestResources.nonmatchingIso());
	}
	private static class RoundtripTemplates {
		FingerprintTemplate extracted;
		FingerprintTemplate roundtripped;
		RoundtripTemplates(FingerprintTemplate extracted, TemplateFormat format) {
			this.extracted = extracted;
			roundtripped = FingerprintCompatibility.importTemplate(FingerprintCompatibility.exportTemplates(format, extracted));
		}
	}
	private void match(RoundtripTemplates probe, RoundtripTemplates candidate, boolean matching) {
		match("native", probe.extracted, candidate.extracted, matching);
		match("roundtripped", probe.roundtripped, candidate.roundtripped, matching);
		match("mixed", probe.extracted, candidate.roundtripped, matching);
	}
	private void match(String kind, FingerprintTemplate probe, FingerprintTemplate candidate, boolean matching) {
		double score = new FingerprintMatcher(probe).match(candidate);
		if (matching)
			assertThat(kind, score, greaterThan(40.0));
		else
			assertThat(kind, score, lessThan(20.0));
	}
	private void roundtrip(TemplateFormat format) {
		RoundtripTemplates probe = new RoundtripTemplates(FingerprintTemplateTest.probe(), format);
		RoundtripTemplates matching = new RoundtripTemplates(FingerprintTemplateTest.matching(), format);
		RoundtripTemplates nonmatching = new RoundtripTemplates(FingerprintTemplateTest.nonmatching(), format);
		match(probe, matching, true);
		match(probe, nonmatching, false);
	}
	@Test
	public void roundtripAnsi378v2004() {
		roundtrip(TemplateFormat.ANSI_378_2004);
	}
	@Test
	public void roundtripAnsi378v2009() {
		roundtrip(TemplateFormat.ANSI_378_2009);
	}
	@Test
	public void roundtripAnsi378v2009AM1() {
		roundtrip(TemplateFormat.ANSI_378_2009_AM1);
	}
	public void roundtripIso19794p2v2005() {
		roundtrip(TemplateFormat.ISO_19794_2_2005);
	}
}
