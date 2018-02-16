package com.machinezoo.sourceafis;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import com.machinezoo.noexception.*;

class TransparencyZip extends FingerprintTransparency {
	private final ZipOutputStream zip;
	private final byte[] buffer = new byte[4096];
	private int offset;
	TransparencyZip(ZipOutputStream zip) {
		this.zip = zip;
	}
	@Override protected void log(String name, Map<String, InputStream> data) {
		++offset;
		Exceptions.sneak().run(() -> {
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
		});
	}
	@Override public void close() {
		super.close();
		Exceptions.sneak().run(zip::close);
	}
}
