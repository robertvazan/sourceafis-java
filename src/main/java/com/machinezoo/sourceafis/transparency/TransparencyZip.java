// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.transparency;

import java.io.*;
import java.util.zip.*;
import com.machinezoo.noexception.*;
import com.machinezoo.sourceafis.*;

public class TransparencyZip extends FingerprintTransparency {
	private final ZipOutputStream zip;
	private int offset;
	public TransparencyZip(OutputStream stream) {
		zip = new ZipOutputStream(stream);
	}
	/*
	 * Synchronize take(), because ZipOutputStream can be accessed only from one thread
	 * while transparency data may flow from multiple threads.
	 */
	@Override
	public synchronized void take(String key, String mime, byte[] data) {
		++offset;
		/*
		 * We allow providing custom output stream, which can fail at any moment.
		 * We however also offer an API that is free of checked exceptions.
		 * We will therefore wrap any checked exceptions from the output stream.
		 */
		Exceptions.wrap().run(() -> {
			zip.putNextEntry(new ZipEntry(String.format("%03d", offset) + "-" + key + TransparencyMimes.suffix(mime)));
			zip.write(data);
			zip.closeEntry();
		});
	}
	@Override
	public void close() {
		super.close();
		Exceptions.wrap().run(zip::close);
	}
}
