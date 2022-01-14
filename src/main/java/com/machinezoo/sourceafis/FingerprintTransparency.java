// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.function.*;
import com.machinezoo.noexception.*;
import com.machinezoo.sourceafis.engine.configuration.*;
import com.machinezoo.sourceafis.engine.transparency.*;

/**
 * Algorithm transparency API that can capture all intermediate data structures produced by SourceAFIS algorithm.
 * See <a href="https://sourceafis.machinezoo.com/transparency/">algorithm transparency</a> pages
 * on SourceAFIS website for more information and a tutorial on how to use this class.
 * <p>
 * Applications can subclass {@code FingerprintTransparency} and override
 * {@link #take(String, String, byte[])} method to define new transparency data logger.
 * One default implementation of {@code FingerprintTransparency} is returned by {@link #zip(OutputStream)} method.
 * Applications can control what transparency data gets produced by overriding {@link #accepts(String)}.
 * <p>
 * {@code FingerprintTransparency} instance should be created in a try-with-resources construct.
 * It will be capturing transparency data from all operations on current thread
 * between invocation of the constructor and invocation of {@link #close()} method,
 * which is called automatically in the try-with-resources construct.
 *
 * @see <a href="https://sourceafis.machinezoo.com/transparency/">Algorithm transparency in SourceAFIS</a>
 */
public abstract class FingerprintTransparency implements CloseableScope {
	/*
	 * API roadmap:
	 * - log()
	 * - capture()
	 */
	static {
		PlatformCheck.run();
	}
	private final TransparencySink sink;
	/**
	 * Creates an instance of {@code FingerprintTransparency} and activates it.
	 * <p>
	 * Activation places the new {@code FingerprintTransparency} instance in thread-local storage,
	 * which causes all operations executed by current thread to log data to this {@code FingerprintTransparency} instance.
	 * If activations are nested, data is only logged to the currently innermost {@code FingerprintTransparency}.
	 * <p>
	 * Deactivation happens in {@link #close()} method.
	 * Instances of {@code FingerprintTransparency} should be created in try-with-resources construct
	 * to ensure that {@link #close()} is always called.
	 * <p>
	 * {@code FingerprintTransparency} is an abstract class.
	 * This constructor is only called by subclasses.
	 * 
	 * @see #close()
	 */
	protected FingerprintTransparency() {
		sink = new TransparencySink(this);
	}
	/**
	 * Filters transparency data keys that can be passed to {@link #take(String, String, byte[])}.
	 * Default implementation always returns {@code true}, i.e. all transparency data is passed to {@link #take(String, String, byte[])}.
	 * Implementation can override this method to filter some keys out, which improves performance.
	 * <p>
	 * This method should always return the same result for the same key.
	 * Result may be cached and this method might not be called every time something is about to be logged.
	 * 
	 * @param key
	 *            transparency data key as used in {@link #take(String, String, byte[])}
	 * @return whether transparency data under given key should be logged
	 * 
	 * @see #take(String, String, byte[])
	 */
	public boolean accepts(String key) {
		/*
		 * Accepting everything by default makes the API easier to use since this method can be ignored.
		 */
		return true;
	}
	/**
	 * Records transparency data. Subclasses must override this method, because the default implementation does nothing.
	 * While this {@code FingerprintTransparency} object is active (between call to the constructor and call to {@link #close()}),
	 * this method is called with transparency data in its parameters.
	 * <p>
	 * Parameter {@code key} specifies the kind of transparency data being logged,
	 * usually corresponding to some stage in the algorithm.
	 * Parameter {@code data} then contains the actual transparency data.
	 * This method may be called multiple times with the same {@code key}
	 * if the algorithm produces that kind of transparency data repeatedly.
	 * See <a href="https://sourceafis.machinezoo.com/transparency/">algorithm transparency</a>
	 * on SourceAFIS website for documentation of the structure of the transparency data.
	 * <p>
	 * Transparency data is offered only if {@link #accepts(String)} returns {@code true} for the same {@code key}.
	 * This allows applications to efficiently collect only transparency data that is actually needed.
	 * <p>
	 * MIME type of the transparency data is provided, which may be useful for generic implementations,
	 * for example transparency data browser app that changes type of visualization based on the MIME type.
	 * Most transparency data is serialized in <a href="https://cbor.io/">CBOR</a> format (MIME application/cbor).
	 * <p>
	 * Implementations of this method should be synchronized. Although the current SourceAFIS algorithm is single-threaded,
	 * future versions of SourceAFIS might run some parts of the algorithm in parallel, which would result in concurrent calls to this method.
	 * <p>
	 * If this method throws, exception is propagated through SourceAFIS code.
	 * 
	 * @param key
	 *            specifies the kind of transparency data being logged
	 * @param mime
	 *            MIME type of the transparency data in {@code data} parameter
	 * @param data
	 *            transparency data being logged
	 * 
	 * @see <a href="https://sourceafis.machinezoo.com/transparency/">Algorithm transparency in SourceAFIS</a>
	 * @see #accepts(String)
	 * @see #zip(OutputStream)
	 */
	public void take(String key, String mime, byte[] data) {
		/*
		 * If nobody overrides this method, assume it is legacy code and call the old capture() method.
		 */
		Map<String, Supplier<byte[]>> map = new HashMap<>();
		map.put(TransparencyMimes.suffix(mime), () -> data);
		capture(key, map);
	}
	/**
	 * Records transparency data (deprecated).
	 * This is a deprecated variant of {@link #take(String, String, byte[])}.
	 * This method is only called if {@link #take(String, String, byte[])} is not overridden.
	 * 
	 * @param key
	 *            specifies the kind of transparency data being reported
	 * @param data
	 *            a map of suffixes (like {@code .cbor} or {@code .dat}) to {@link Supplier} of the available transparency data
	 * 
	 * @see #take(String, String, byte[])
	 * @deprecated
	 */
	@Deprecated
	protected void capture(String key, Map<String, Supplier<byte[]>> data) {
		/*
		 * If nobody overrode this method, assume it is legacy code and call the old log() method.
		 */
		Map<String, Supplier<ByteBuffer>> translated = new HashMap<>();
		for (Map.Entry<String, Supplier<byte[]>> entry : data.entrySet())
			translated.put(entry.getKey(), () -> ByteBuffer.wrap(entry.getValue().get()));
		log(key, translated);
	}
	/**
	 * Records transparency data (deprecated).
	 * This is a deprecated variant of {@link #take(String, String, byte[])}.
	 * This method is only called if {@link #take(String, String, byte[])} and {@link #capture(String, Map)} are not overridden.
	 * 
	 * @param key
	 *            specifies the kind of transparency data being reported
	 * @param data
	 *            a map of suffixes (like {@code .cbor} or {@code .dat}) to {@link Supplier} of the available transparency data
	 * 
	 * @see #take(String, String, byte[])
	 * @deprecated
	 */
	@Deprecated
	protected void log(String key, Map<String, Supplier<ByteBuffer>> data) {
	}
	/**
	 * Deactivates transparency logging and releases system resources held by this instance if any.
	 * This method is normally called automatically when {@code FingerprintTransparency} is used in try-with-resources construct.
	 * <p>
	 * Deactivation stops transparency data logging to this instance of {@code FingerprintTransparency}.
	 * Logging thus takes place between invocation of constructor ({@link #FingerprintTransparency()}) and invocation of this method.
	 * If activations were nested, this method reactivates the outer {@code FingerprintTransparency}.
	 * <p>
	 * Subclasses can override this method to perform cleanup.
	 * Default implementation of this method performs deactivation.
	 * It must be called by overriding methods for deactivation to work correctly.
	 * 
	 * @see #FingerprintTransparency()
	 */
	@Override
	public void close() {
		sink.close();
	}
	/**
	 * Writes all transparency data to a ZIP file.
	 * This is a convenience method to enable easy exploration of the available data.
	 * Programmatic processing of transparency data should be done by subclassing {@code FingerprintTransparency}
	 * and overriding {@link #take(String, String, byte[])} method.
	 * <p>
	 * ZIP file entries have filenames like {@code NNN-key.ext} where {@code NNN} is a sequentially assigned ID,
	 * {@code key} comes from {@link #take(String, String, byte[])} parameter, and {@code ext} is derived from MIME type.
	 * <p>
	 * The returned {@code FingerprintTransparency} object holds system resources
	 * and callers are responsible for calling {@link #close()} method, perhaps using try-with-resources construct.
	 * Failure to close the returned {@code FingerprintTransparency} instance may result in damaged ZIP file.
	 * <p>
	 * If the provided {@code stream} throws {@link IOException},
	 * the exception will be wrapped in an unchecked exception and propagated.
	 * 
	 * @param stream
	 *            output stream where ZIP file will be written (will be closed when the returned {@code FingerprintTransparency} is closed)
	 * @return algorithm transparency logger that writes data to a ZIP file
	 * 
	 * @see <a href="https://sourceafis.machinezoo.com/transparency/">Algorithm transparency in SourceAFIS</a>
	 * @see #close()
	 * @see #take(String, String, byte[])
	 */
	public static FingerprintTransparency zip(OutputStream stream) {
		return new TransparencyZip(stream);
	}
}
