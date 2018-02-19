// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.function.*;
import java.util.zip.*;
import com.machinezoo.noexception.*;

class TransparencyZip extends FingerprintTransparency {
	private final ZipOutputStream zip;
	private int offset;
	TransparencyZip(ZipOutputStream zip) {
		this.zip = zip;
	}
	@Override protected void log(String name, Map<String, Supplier<ByteBuffer>> data) {
		++offset;
		Exceptions.sneak().run(() -> {
			for (String suffix : data.keySet()) {
				zip.putNextEntry(new ZipEntry(String.format("%02d", offset) + "-" + name + suffix));
				ByteBuffer buffer = data.get(suffix).get();
				WritableByteChannel output = Channels.newChannel(zip);
				while (buffer.hasRemaining())
					output.write(buffer);
				zip.closeEntry();
			}
		});
	}
	@Override public void close() {
		super.close();
		Exceptions.sneak().run(zip::close);
	}
}
