// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import org.apache.sanselan.*;
import org.jnbis.api.*;
import org.jnbis.api.model.*;
import com.machinezoo.noexception.*;

abstract class ImageDecoder {
	private static class DecodedImage {
		int width;
		int height;
		int[] argb;
	}
	abstract String name();
	abstract DecodedImage decode(byte[] image);
	private static final List<ImageDecoder> all = Arrays.asList(
		new ImageIODecoder(),
		new SanselanDecoder(),
		new WsqDecoder());
	static DoubleMap toDoubleMap(byte[] image) {
		Map<ImageDecoder, Throwable> exceptions = new HashMap<>();
		for (ImageDecoder decoder : all) {
			try {
				DecodedImage decoded = decoder.decode(image);
				DoubleMap map = new DoubleMap(decoded.width, decoded.height);
				for (int y = 0; y < decoded.height; ++y) {
					for (int x = 0; x < decoded.width; ++x) {
						int pixel = decoded.argb[y * decoded.width + x];
						int color = (pixel & 0xff) + ((pixel >> 8) & 0xff) + ((pixel >> 16) & 0xff);
						map.set(x, y, 1 - color * (1.0 / (3.0 * 255.0)));
					}
				}
				return map;
			} catch (Throwable ex) {
				exceptions.put(decoder, ex);
			}
		}
		throw new IllegalArgumentException(String.format("Unsupported image format [%s].", all.stream()
			.map(d -> String.format("%s = '%s'", d.name(), exceptions.get(d)))
			.collect(joining(", "))));
	}
	private static class ImageIODecoder extends ImageDecoder {
		@Override String name() {
			return "ImageIO";
		}
		@Override DecodedImage decode(byte[] image) {
			return Exceptions.sneak().get(() -> {
				BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(image));
				if (buffered == null)
					throw new IllegalArgumentException("Unsupported image format.");
				DecodedImage decoded = new DecodedImage();
				decoded.width = buffered.getWidth();
				decoded.height = buffered.getHeight();
				decoded.argb = new int[decoded.width * decoded.height];
				buffered.getRGB(0, 0, decoded.width, decoded.height, decoded.argb, 0, decoded.width);
				return decoded;
			});
		}
	}
	private static class SanselanDecoder extends ImageDecoder {
		@Override String name() {
			return "Sanselan";
		}
		@Override DecodedImage decode(byte[] image) {
			return Exceptions.sneak().get(() -> {
				BufferedImage buffered = Sanselan.getBufferedImage(new ByteArrayInputStream(image));
				DecodedImage decoded = new DecodedImage();
				decoded.width = buffered.getWidth();
				decoded.height = buffered.getHeight();
				decoded.argb = new int[decoded.width * decoded.height];
				buffered.getRGB(0, 0, decoded.width, decoded.height, decoded.argb, 0, decoded.width);
				return decoded;
			});
		}
	}
	private static class WsqDecoder extends ImageDecoder {
		@Override String name() {
			return "WSQ";
		}
		@Override DecodedImage decode(byte[] image) {
			if (image.length < 2 || image[0] != (byte)0xff || image[1] != (byte)0xa0)
				throw new IllegalArgumentException("This is not a WSQ image.");
			return Exceptions.sneak().get(() -> {
				Bitmap bitmap = Jnbis.wsq().decode(image).asBitmap();
				DecodedImage decoded = new DecodedImage();
				decoded.width = bitmap.getWidth();
				decoded.height = bitmap.getHeight();
				byte[] buffer = bitmap.getPixels();
				decoded.argb = new int[decoded.width * decoded.height];
				for (int y = 0; y < decoded.height; ++y) {
					for (int x = 0; x < decoded.width; ++x) {
						int gray = buffer[y * decoded.width + x] & 0xff;
						decoded.argb[y * decoded.width + x] = 0xff00_0000 | (gray << 16) | (gray << 8) | gray;
					}
				}
				return decoded;
			});
		}
	}
}
