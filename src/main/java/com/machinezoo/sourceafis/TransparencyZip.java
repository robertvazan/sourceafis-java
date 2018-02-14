package com.machinezoo.sourceafis;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import lombok.*;

@RequiredArgsConstructor class TransparencyZip extends FingerprintTransparency {
	private final ZipOutputStream zip;
	private final byte[] buffer = new byte[4096];
	private int offset;
	@Override @SneakyThrows protected void log(String name, Map<String, InputStream> data) {
		++offset;
		for (String suffix : data.keySet()) {
			zip.putNextEntry(new ZipEntry(String.format("%02d", offset) + "-" + name + suffix));
			InputStream stream = data.get(suffix);
			while (true) {
				int read = stream.read(buffer);
				if (read <= 0)
					break;
				zip.write(buffer, 0, read);
			}
			zip.closeEntry();
		}
	}
	@Override @SneakyThrows public void close() {
		super.close();
		zip.close();
	}
}
