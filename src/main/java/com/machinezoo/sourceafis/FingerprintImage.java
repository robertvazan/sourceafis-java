// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.util.*;
import javax.imageio.*;

/**
 * Pixels and metadata of the fingerprint image.
 * This class captures all fingerprint information that is available prior to construction of {@link FingerprintTemplate}.
 * Since SourceAFIS algorithm is not scale-invariant, all images should have DPI configured explicitly by calling {@link #dpi(double)}.
 * <p>
 * Application should start fingerprint processing by constructing an instance of {@code FingerprintImage}
 * and then passing it to {@link FingerprintTemplate#FingerprintTemplate(FingerprintImage)}.
 * <p>
 * Fingerprint image can be either in one of the supported image formats (PNG, JPEG, ...),
 * in which case method {@link #decode(byte[])} is used,
 * or it can be a raw grayscale image, for which method {@link #grayscale(int, int, byte[])} is used.
 * 
 * @see FingerprintTemplate
 */
public class FingerprintImage {
	/*
	 * API roadmap:
	 * + position(FingerprintPosition)
	 * + other fingerprint properties
	 */
	/**
	 * Create new container for fingerprint image data.
	 * The newly constructed instance cannot be used to create {@link FingerprintTemplate}
	 * until at least pixel data is provided by calling {@link #decode(byte[])} or {@link #grayscale(int, int, byte[])}.
	 * 
	 * @see #decode(byte[])
	 * @see #grayscale(int, int, byte[])
	 */
	public FingerprintImage() {
	}
	double dpi = 500;
	/**
	 * Set DPI (dots per inch) of the fingerprint image.
	 * This is the DPI of the image passed to {@link #decode(byte[])} or {@link #grayscale(int, int, byte[])}.
	 * Check your fingerprint reader specification for correct DPI value. Default DPI is 500.
	 * 
	 * @param dpi
	 *            DPI of the fingerprint image, usually around 500
	 * @return {@code this} (fluent method)
	 * @throws IllegalArgumentException
	 *             if {@code dpi} is non-positive, impossibly low, or impossibly high
	 * 
	 * @see #decode(byte[])
	 */
	public FingerprintImage dpi(double dpi) {
		if (dpi < 20 || dpi > 20_000)
			throw new IllegalArgumentException();
		this.dpi = dpi;
		return this;
	}
	DoubleMatrix matrix;
	/**
	 * Decode fingerprint image in standard format.
	 * The image must contain black fingerprint on white background at the DPI specified by calling {@link #dpi(double)}.
	 * <p>
	 * The image may be in any format commonly used to store fingerprint images, including PNG, JPEG, BMP, TIFF, or WSQ.
	 * SourceAFIS will try to decode the image using Java's {@link ImageIO} (PNG, JPEG, BMP),
	 * <a href="https://commons.apache.org/proper/commons-imaging/">Sanselan</a> library (TIFF),
	 * <a href="https://github.com/kareez/jnbis">JNBIS</a> library (WSQ), and Android's
	 * <a href="https://developer.android.com/reference/android/graphics/Bitmap">Bitmap</a> class (PNG, JPEG, BMP) in this order.
	 * Note that these libraries might not support all versions and variations of the mentioned formats.
	 * 
	 * @param image
	 *            fingerprint image in one of the supported formats
	 * @return {@code this} (fluent method)
	 * @throws NullPointerException
	 *             if {@code image} is {@code null}
	 * @throws IllegalArgumentException
	 *             if the image format is unsupported or the image is corrupted
	 * 
	 * @see #dpi(double)
	 * @see #grayscale(int, int, byte[])
	 * @see FingerprintCompatibility#convert(byte[])
	 * @see FingerprintTemplate#FingerprintTemplate(byte[])
	 */
	public FingerprintImage decode(byte[] image) {
		Objects.requireNonNull(image);
		ImageDecoder.DecodedImage decoded = ImageDecoder.decodeAny(image);
		matrix = new DoubleMatrix(decoded.width, decoded.height);
		for (int y = 0; y < decoded.height; ++y) {
			for (int x = 0; x < decoded.width; ++x) {
				int pixel = decoded.pixels[y * decoded.width + x];
				int color = (pixel & 0xff) + ((pixel >> 8) & 0xff) + ((pixel >> 16) & 0xff);
				matrix.set(x, y, 1 - color * (1.0 / (3.0 * 255.0)));
			}
		}
		return this;
	}
	/**
	 * Load raw grayscale fingerprint image from byte array.
	 * The image must contain black fingerprint on white background at the DPI specified by calling {@link #dpi(double)}.
	 * <p>
	 * Pixels are represented as 8-bit unsigned bytes with 0 meaning black and 255 meaning white.
	 * Java's byte is a signed 8-bit number, but this method interprets all 8 bits as an unsigned number
	 * as if by calling {@link Byte#toUnsignedInt(byte)}.
	 * Pixels in {@code pixels} array are ordered from top-left to bottom-right in horizontal rows.
	 * Size of {@code pixels} must be equal to {@code width * height}.
	 * 
	 * @param width
	 *            width of the image
	 * @param height
	 *            height of the image
	 * @param pixels
	 *            image pixels ordered from top-left to bottom-right in horizontal rows
	 * @return {@code this} (fluent method)
	 * @throws NullPointerException
	 *             if {@code image} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code width} or {@code height} is not positive or if {@code pixels} length is not {@code width * height}
	 * 
	 * @see #dpi(double)
	 * @see #decode(byte[])
	 */
	public FingerprintImage grayscale(int width, int height, byte[] pixels) {
		Objects.requireNonNull(pixels);
		if (width <= 0 || height <= 0 || pixels.length != width * height)
			throw new IndexOutOfBoundsException();
		matrix = new DoubleMatrix(width, height);
		for (int y = 0; y < height; ++y)
			for (int x = 0; x < width; ++x)
				matrix.set(x, y, 1 - Byte.toUnsignedInt(pixels[y * width + x]) / 255.0);
		return this;
	}
}
