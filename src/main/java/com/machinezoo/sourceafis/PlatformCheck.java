// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.io.*;
import java.util.regex.*;
import org.apache.commons.io.*;
import com.machinezoo.noexception.*;

class PlatformCheck {
	// https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
	private static final Pattern versionRe1 = Pattern.compile("1\\.([0-9]{1,3})\\..*");
	private static final Pattern versionRe2 = Pattern.compile("([0-9]{1,3})\\..*");
	private static void requireJava() {
		String version = System.getProperty("java.version");
		/*
		 * Property java.version should be always present, but let's guard against weird Java implementations.
		 */
		if (version != null) {
			Matcher matcher = versionRe1.matcher(version);
			if (!matcher.matches()) {
				matcher = versionRe2.matcher(version);
				/*
				 * If no version pattern matches, we are running on Android or in some other weird JVM.
				 * Since the version check does not work, we will just skip it.
				 */
				if (!matcher.matches())
					return;
			}
			/*
			 * Parsing will not throw, because we constrain the version to [0-9]{1,k} in the regex.
			 */
			int major = Integer.parseInt(matcher.group(1));
			if (major < 8)
				throw new RuntimeException("SourceAFIS requires Java 8 or higher. Currently running JRE " + version + ".");
		}
	}
	/*
	 * Eager checks should be executed automatically before lazy checks.
	 */
	static {
		requireJava();
	}
	/*
	 * Called to trigger eager checks above. Call to run() is placed in several places to ensure nothing runs before checks are complete.
	 * Some code runs even before the static initializers that trigger call of this method. We cannot be 100% sure that platform check runs first.
	 */
	static void run() {
	}
	static byte[] resource(String filename) {
		return Exceptions.wrap(ex -> new IllegalStateException("Cannot read SourceAFIS resource: " + filename + ". Use proper dependency management tool.", ex)).get(() -> {
			try (InputStream stream = PlatformCheck.class.getResourceAsStream(filename)) {
				if (stream == null)
					throw new IllegalStateException("SourceAFIS resource not found: " + filename + ". Use proper dependency management tool.");
				return IOUtils.toByteArray(stream);
			}
		});
	}
	static boolean hasClass(String name) {
		try {
			Class.forName(name);
			return true;
		} catch (Throwable ex) {
			return false;
		}
	}
}
