package com.machinezoo.sourceafis;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.function.*;
import com.google.gson.*;

class LazyByteStream extends InputStream {
	private final Supplier<ByteBuffer> producer;
	private ByteBuffer buffer;
	LazyByteStream(Supplier<ByteBuffer> producer) {
		this.producer = producer;
	}
	static LazyByteStream json(Supplier<Object> source) {
		return new LazyByteStream(() -> ByteBuffer.wrap(new Gson().toJson(source.get()).getBytes(StandardCharsets.UTF_8)));
	}
	@Override public int read() throws IOException {
		if (buffer == null)
			buffer = producer.get();
		if (!buffer.hasRemaining())
			return -1;
		return buffer.get();
	}
}