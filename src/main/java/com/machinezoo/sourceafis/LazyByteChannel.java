// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.function.*;
import com.google.gson.*;

class LazyByteChannel implements ReadableByteChannel {
	private final Supplier<ByteBuffer> producer;
	private ByteBuffer source;
	private boolean open = true;
	LazyByteChannel(Supplier<ByteBuffer> producer) {
		this.producer = producer;
	}
	static LazyByteChannel json(Supplier<Object> source) {
		return new LazyByteChannel(() -> ByteBuffer.wrap(new GsonBuilder().setPrettyPrinting().create().toJson(source.get()).getBytes(StandardCharsets.UTF_8)));
	}
	@Override public boolean isOpen() {
		return open;
	}
	@Override public void close() throws IOException {
		open = false;
	}
	@Override public int read(ByteBuffer destination) throws IOException {
		if (!open)
			throw new ClosedChannelException();
		if (source == null)
			source = producer.get();
		if (!source.hasRemaining())
			return -1;
		int count = 0;
		while (source.hasRemaining() && destination.hasRemaining()) {
			destination.put(source.get());
			++count;
		}
		return count;
	}
}
