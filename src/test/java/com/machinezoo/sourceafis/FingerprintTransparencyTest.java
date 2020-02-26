// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import java.util.*;
import java.util.function.*;
import org.junit.jupiter.api.*;

public class FingerprintTransparencyTest {
	private static class TransparencyChecker extends FingerprintTransparency {
		final List<String> keywords = new ArrayList<>();
		@Override protected void capture(String keyword, Map<String, Supplier<byte[]>> data) {
			keywords.add(keyword);
			for (Map.Entry<String, Supplier<byte[]>> entry : data.entrySet()) {
				assertThat(keyword, entry.getKey(), is(oneOf(".json", ".dat")));
				assertThat(keyword, entry.getValue().get().length, greaterThan(0));
			}
		}
	}
	@Test public void versioned() {
		try (TransparencyChecker transparency = new TransparencyChecker()) {
			new FingerprintTemplate(FingerprintImageTest.probe());
			assertThat(transparency.keywords, hasItem("version"));
		}
	}
	@Test public void extractor() {
		try (TransparencyChecker transparency = new TransparencyChecker()) {
			new FingerprintTemplate(FingerprintImageTest.probe());
			assertThat(transparency.keywords, is(not(empty())));
		}
	}
	@Test public void matcher() {
		FingerprintTemplate probe = FingerprintTemplateTest.probe();
		FingerprintTemplate matching = FingerprintTemplateTest.matching();
		new FingerprintTemplate(FingerprintImageTest.probe());
		try (TransparencyChecker transparency = new TransparencyChecker()) {
			new FingerprintMatcher()
				.index(probe)
				.match(matching);
		}
	}
	@Test public void deserialization() {
		byte[] serialized = FingerprintTemplateTest.probe().toByteArray();
		try (TransparencyChecker transparency = new TransparencyChecker()) {
			new FingerprintTemplate(serialized);
		}
	}
}
