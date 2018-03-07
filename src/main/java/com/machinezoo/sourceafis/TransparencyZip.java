// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.function.*;
import java.util.zip.*;
import com.machinezoo.noexception.*;

class TransparencyZip extends FingerprintTransparency {
	private final ZipOutputStream zip;
	private int offset;
	TransparencyZip(OutputStream stream) {
		zip = new ZipOutputStream(stream);
	}
	@Override protected void log(String keyword, Map<String, Supplier<ByteBuffer>> data) {
		Exceptions.sneak().run(() -> {
			List<String> suffixes = data.keySet().stream()
				.sorted(Comparator.comparing(ext -> {
					if (ext.equals(".json"))
						return 1;
					if (ext.equals(".dat"))
						return 2;
					return 3;
				}))
				.collect(toList());
			for (String suffix : suffixes) {
				++offset;
				zip.putNextEntry(new ZipEntry(String.format("%03d", offset) + "-" + keyword + suffix));
				ByteBuffer buffer = data.get(suffix).get();
				WritableByteChannel output = Channels.newChannel(zip);
				while (buffer.hasRemaining())
					output.write(buffer);
				zip.closeEntry();
			}
		});
	}
	@Override public void close() {
		Exceptions.sneak().run(zip::close);
	}
}
