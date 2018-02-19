// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.*;
import com.machinezoo.noexception.*;

class TransparencyZip extends FingerprintTransparency {
	private final ZipOutputStream zip;
	private final ByteBuffer buffer = ByteBuffer.allocate(4096);
	private int offset;
	TransparencyZip(ZipOutputStream zip) {
		this.zip = zip;
	}
	@Override protected void log(String name, Map<String, ReadableByteChannel> data) {
		++offset;
		Exceptions.sneak().run(() -> {
			for (String suffix : data.keySet()) {
				zip.putNextEntry(new ZipEntry(String.format("%02d", offset) + "-" + name + suffix));
				try (ReadableByteChannel input = data.get(suffix)) {
					WritableByteChannel output = Channels.newChannel(zip);
					while (true) {
						buffer.clear();
						if (input.read(buffer) < 0)
							break;
						buffer.flip();
						while (buffer.hasRemaining())
							output.write(buffer);
					}
				}
				zip.closeEntry();
			}
		});
	}
	@Override public void close() {
		super.close();
		Exceptions.sneak().run(zip::close);
	}
}
