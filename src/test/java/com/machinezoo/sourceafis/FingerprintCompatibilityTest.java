// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import java.util.function.*;
import org.junit.*;

public class FingerprintCompatibilityTest {
	@Test public void version() {
		assertThat(FingerprintCompatibility.version(), matchesPattern("^\\d+\\.\\d+\\.\\d+$"));
	}
	public static FingerprintTemplate probeIso() {
		return FingerprintCompatibility.convert(TestResources.probeIso());
	}
	public static FingerprintTemplate matchingIso() {
		return FingerprintCompatibility.convert(TestResources.matchingIso());
	}
	public static FingerprintTemplate nonmatchingIso() {
		return FingerprintCompatibility.convert(TestResources.nonmatchingIso());
	}
	private static class RoundtripTemplates {
		FingerprintTemplate extracted;
		FingerprintTemplate roundtripped;
		RoundtripTemplates(FingerprintTemplate extracted, Function<FingerprintTemplate[], byte[]> exporter) {
			this.extracted = extracted;
			roundtripped = FingerprintCompatibility.convert(exporter.apply(new FingerprintTemplate[] { extracted }));;
		}
	}
	private void match(RoundtripTemplates probe, RoundtripTemplates candidate, boolean matching) {
		match("native", probe.extracted, candidate.extracted, matching);
		match("roundtripped", probe.roundtripped, candidate.roundtripped, matching);
		match("mixed", probe.extracted, candidate.roundtripped, matching);
	}
	private void match(String kind, FingerprintTemplate probe, FingerprintTemplate candidate, boolean matching) {
		double score = new FingerprintMatcher().index(probe).match(candidate);
		if (matching)
			assertThat(kind, score, greaterThan(40.0));
		else
			assertThat(kind,score, lessThan(20.0));
	}
	private void roundtrip(Function<FingerprintTemplate[], byte[]> exporter) {
		RoundtripTemplates probe = new RoundtripTemplates(FingerprintTemplateTest.probe(), exporter);
		RoundtripTemplates matching = new RoundtripTemplates(FingerprintTemplateTest.matching(), exporter);
		RoundtripTemplates nonmatching = new RoundtripTemplates(FingerprintTemplateTest.nonmatching(), exporter);
		match(probe, matching, true);
		match(probe, nonmatching, false);
	}
	@Test public void roundtripAnsi378v2004() {
		roundtrip(FingerprintCompatibility::toAnsiIncits378v2004);
	}
	@Test public void roundtripAnsi378v2009() {
		roundtrip(FingerprintCompatibility::toAnsiIncits378v2009);
	}
	@Test public void roundtripAnsi378v2009AM1() {
		roundtrip(FingerprintCompatibility::toAnsiIncits378v2009AM1);
	}
}
