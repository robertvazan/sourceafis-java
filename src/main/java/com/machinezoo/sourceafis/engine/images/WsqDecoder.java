// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.images;

import org.jnbis.api.*;
import org.jnbis.api.model.*;
import com.machinezoo.noexception.*;

/*
 * WSQ is often used to compress fingerprints, which is why JNBIS WSQ decoder is very valuable.
 */
class WsqDecoder extends ImageDecoder {
	@Override
	public boolean available() {
		/*
		 * JNBIS WSQ decoder is pure Java, which means it is always available.
		 */
		return true;
	}
	@Override
	public String name() {
		return "WSQ";
	}
	@Override
	public DecodedImage decode(byte[] image) {
		if (image.length < 2 || image[0] != (byte)0xff || image[1] != (byte)0xa0)
			throw new IllegalArgumentException("This is not a WSQ image.");
		return Exceptions.sneak().get(() -> {
			Bitmap bitmap = Jnbis.wsq().decode(image).asBitmap();
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			byte[] buffer = bitmap.getPixels();
			int[] pixels = new int[width * height];
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					int gray = buffer[y * width + x] & 0xff;
					pixels[y * width + x] = 0xff00_0000 | (gray << 16) | (gray << 8) | gray;
				}
			}
			return new DecodedImage(width, height, pixels);
		});
	}
}
