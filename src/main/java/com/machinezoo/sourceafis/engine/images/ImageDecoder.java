// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.images;

import static java.util.stream.Collectors.*;
import java.util.*;

/*
 * We cannot just use ImageIO, because fingerprints often come in formats not supported by ImageIO.
 * We would also like SourceAFIS to work out of the box on Android, which doesn't have ImageIO at all.
 * For these reasons, we have several image decoders that are tried in order for every image.
 * 
 * This should really be a separate image decoding library, but AFAIK there is no such universal library.
 * Perhaps one should be created by forking this code off SourceAFIS and expanding it considerably.
 * Such library can be generalized to suit many applications by making supported formats
 * configurable via maven's provided dependencies.
 */
public abstract class ImageDecoder {
	/*
	 * This is used to check whether the image decoder implementation exists.
	 * If it does not, we can produce understandable error message instead of ClassNotFoundException.
	 */
	public abstract boolean available();
	public abstract String name();
	/*
	 * Decoding method never returns null. It throws if it fails to decode the template,
	 * including cases when the decoder simply doesn't support the image format.
	 */
	public abstract DecodedImage decode(byte[] image);
	/*
	 * Order is significant. If multiple decoders support the format, the first one wins.
	 * This list is ordered to favor more common image formats and more common image decoders.
	 * This makes sure that SourceAFIS performs equally well for common formats and common decoders
	 * regardless of how many special-purpose decoders are added to this list.
	 */
	private static final List<ImageDecoder> ALL = Arrays.asList(
		new ImageIODecoder(),
		new WsqDecoder(),
		new AndroidImageDecoder());
	public static DecodedImage decodeAny(byte[] image) {
		Map<ImageDecoder, Throwable> exceptions = new HashMap<>();
		for (ImageDecoder decoder : ALL) {
			try {
				if (!decoder.available())
					throw new UnsupportedOperationException("Image decoder is not available.");
				return decoder.decode(image);
			} catch (Throwable ex) {
				exceptions.put(decoder, ex);
			}
		}
		/*
		 * We should create an exception type that contains a lists of exceptions from all decoders.
		 * But for now we don't want to complicate SourceAFIS API.
		 * It will wait until this code gets moved to a separate image decoding library.
		 * For now, we just summarize all the exceptions in a long message.
		 */
		throw new IllegalArgumentException(String.format("Unsupported image format [%s].", ALL.stream()
			.map(d -> String.format("%s = '%s'", d.name(), formatError(exceptions.get(d))))
			.collect(joining(", "))));
	}
	private static String formatError(Throwable exception) {
		List<Throwable> ancestors = new ArrayList<>();
		for (Throwable ancestor = exception; ancestor != null; ancestor = ancestor.getCause())
			ancestors.add(ancestor);
		return ancestors.stream()
			.map(ex -> ex.toString())
			.collect(joining(" -> "));
	}
}
