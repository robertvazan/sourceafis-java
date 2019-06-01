// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import java.util.function.*;
import org.junit.*;

public class FingerprintCompatibilityTest {
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
			assertTrue("Score (" + kind + "): " + score, score > 40);
		else
			assertTrue("Score (" + kind + "): " + score, score < 20);
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
