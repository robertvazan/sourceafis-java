// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.images;

import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import com.machinezoo.noexception.*;
import com.machinezoo.sourceafis.engine.configuration.*;

/*
 * Image decoder using built-in ImageIO from JRE.
 * While ImageIO has its own extension mechanism, theoretically supporting any format,
 * this extension mechanism is cumbersome and on Android the whole ImageIO is missing.
 */
class ImageIODecoder extends ImageDecoder {
	@Override
	public boolean available() {
		return PlatformCheck.hasClass("javax.imageio.ImageIO");
	}
	@Override
	public String name() {
		return "ImageIO";
	}
	@Override
	public DecodedImage decode(byte[] image) {
		return Exceptions.sneak().get(() -> {
			BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(image));
			if (buffered == null)
				throw new IllegalArgumentException("Unsupported image format.");
			int width = buffered.getWidth();
			int height = buffered.getHeight();
			int[] pixels = new int[width * height];
			buffered.getRGB(0, 0, width, height, pixels, 0, width);
			return new DecodedImage(width, height, pixels);
		});
	}
}
