// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import java.util.*;
import org.junit.jupiter.api.*;

public class FingerprintTransparencyTest {
	private static class TransparencyChecker extends FingerprintTransparency {
		final List<String> keys = new ArrayList<>();
		@Override
		public void take(String key, String mime, byte[] data) {
			keys.add(key);
			assertThat(key, mime, is(oneOf("application/cbor", "text/plain")));
			assertThat(key, data.length, greaterThan(0));
		}
	}
	@Test
	public void versioned() {
		try (TransparencyChecker transparency = new TransparencyChecker()) {
			new FingerprintTemplate(FingerprintImageTest.probe());
			assertThat(transparency.keys, hasItem("version"));
		}
	}
	@Test
	public void extractor() {
		try (TransparencyChecker transparency = new TransparencyChecker()) {
			new FingerprintTemplate(FingerprintImageTest.probe());
			assertThat(transparency.keys, is(not(empty())));
		}
	}
	@Test
	public void matcher() {
		FingerprintTemplate probe = FingerprintTemplateTest.probe();
		FingerprintTemplate matching = FingerprintTemplateTest.matching();
		new FingerprintTemplate(FingerprintImageTest.probe());
		try (TransparencyChecker transparency = new TransparencyChecker()) {
			new FingerprintMatcher()
				.index(probe)
				.match(matching);
			assertThat(transparency.keys, is(not(empty())));
		}
	}
	@Test
	public void deserialization() {
		byte[] serialized = FingerprintTemplateTest.probe().toByteArray();
		try (TransparencyChecker transparency = new TransparencyChecker()) {
			new FingerprintTemplate(serialized);
			assertThat(transparency.keys, is(not(empty())));
		}
	}
	private static class TransparencyFilter extends FingerprintTransparency {
		final List<String> keys = new ArrayList<>();
		@Override
		public boolean accepts(String key) {
			return false;
		}
		@Override
		public void take(String key, String mime, byte[] data) {
			keys.add(key);
		}
	}
	@Test
	public void filtered() {
		try (TransparencyFilter transparency = new TransparencyFilter()) {
			new FingerprintMatcher()
				.index(new FingerprintTemplate(FingerprintImageTest.probe()))
				.match(FingerprintTemplateTest.matching());
			assertThat(transparency.keys, is(empty()));
		}
	}
}
