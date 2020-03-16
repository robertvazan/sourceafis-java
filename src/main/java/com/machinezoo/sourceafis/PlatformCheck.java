package com.machinezoo.sourceafis;

import java.io.*;
import org.apache.commons.io.*;
import com.machinezoo.noexception.*;

class PlatformCheck {
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
