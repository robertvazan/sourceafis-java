// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.util.*;
import javax.imageio.*;

/**
 * Pixels and metadata of the fingerprint image.
 * This class captures all fingerprint information that is available prior to construction of {@link FingerprintTemplate}.
 * It consists of pixel data and additional information in {@link FingerprintImageOptions}.
 * Since SourceAFIS algorithm is not scale-invariant, all images should have DPI
 * configured explicitly by calling {@link FingerprintImageOptions#dpi(double)}.
 * <p>
 * Application should start fingerprint processing by constructing an instance of {@code FingerprintImage}
 * and then passing it to {@link FingerprintTemplate#FingerprintTemplate(FingerprintImage)}.
 * <p>
 * Fingerprint image can be either in one of the supported image formats (PNG, JPEG, ...),
 * in which case constructor {@link #FingerprintImage(byte[], FingerprintImageOptions)} is used,
 * or it can be a raw grayscale image, for which constructor
 * {@link #FingerprintImage(int, int, byte[], FingerprintImageOptions)} is used.
 * 
 * @see FingerprintImageOptions
 * @see FingerprintTemplate
 */
public class FingerprintImage {
	/*
	 * API roadmap:
	 * + double dpi()
	 * + int width()
	 * + int height()
	 * + byte[] grayscale()
	 * + FingerprintPosition position()
	 */
	static {
		PlatformCheck.run();
	}
	double dpi = 500;
	DoubleMatrix matrix;
	/**
	 * Decodes fingerprint image in standard format.
	 * The image must contain black fingerprint on white background
	 * in resolution specified by calling {@link FingerprintImageOptions#dpi(double)}.
	 * <p>
	 * The image may be in any format commonly used to store fingerprint images, including PNG, JPEG, BMP, TIFF, or WSQ.
	 * SourceAFIS will try to decode the image using Java's {@link ImageIO} (PNG, JPEG, BMP, and on Java 9+ TIFF),
	 * <a href="https://github.com/kareez/jnbis">JNBIS</a> library (WSQ), and Android's
	 * <a href="https://developer.android.com/reference/android/graphics/Bitmap">Bitmap</a> class (PNG, JPEG, BMP) in this order.
	 * Note that these libraries might not support all versions and variations of the mentioned formats.
	 * 
	 * @param image
	 *            fingerprint image in one of the supported formats
	 * @param options
	 *            additional information about the image or {@code null} for default options
	 * @throws NullPointerException
	 *             if {@code image} is {@code null}
	 * @throws IllegalArgumentException
	 *             if the image format is unsupported or the image is corrupted
	 * 
	 * @see #FingerprintImage(int, int, byte[], FingerprintImageOptions)
	 * @see FingerprintCompatibility#convert(byte[])
	 * @see FingerprintTemplate#FingerprintTemplate(byte[])
	 */
	public FingerprintImage(byte[] image, FingerprintImageOptions options) {
		Objects.requireNonNull(image);
		if (options == null)
			options = new FingerprintImageOptions();
		dpi = options.dpi;
		ImageDecoder.DecodedImage decoded = ImageDecoder.decodeAny(image);
		matrix = new DoubleMatrix(decoded.width, decoded.height);
		for (int y = 0; y < decoded.height; ++y) {
			for (int x = 0; x < decoded.width; ++x) {
				int pixel = decoded.pixels[y * decoded.width + x];
				int color = (pixel & 0xff) + ((pixel >> 8) & 0xff) + ((pixel >> 16) & 0xff);
				matrix.set(x, y, 1 - color * (1.0 / (3.0 * 255.0)));
			}
		}
	}
	/**
	 * Decodes fingerprint image in standard format using default options.
	 * This constructor is equivalent to calling {@link #FingerprintImage(byte[], FingerprintImageOptions)}
	 * with default {@link FingerprintImageOptions}.
	 * 
	 * @param image
	 *            fingerprint image in one of the supported formats
	 * @throws NullPointerException
	 *             if {@code image} is {@code null}
	 * @throws IllegalArgumentException
	 *             if the image format is unsupported or the image is corrupted
	 * 
	 * @see #FingerprintImage(int, int, byte[])
	 * @see FingerprintCompatibility#convert(byte[])
	 * @see FingerprintTemplate#FingerprintTemplate(byte[])
	 */
	public FingerprintImage(byte[] image) {
		this(image, null);
	}
	/**
	 * Reads raw grayscale fingerprint image from byte array.
	 * The image must contain black fingerprint on white background
	 * in resolution specified by calling {@link FingerprintImageOptions#dpi(double)}.
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
	 * @param options
	 *            additional information about the image or {@code null} for default options
	 * @throws NullPointerException
	 *             if {@code pixels} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code width} or {@code height} is not positive or if {@code pixels} length is not {@code width * height}
	 * 
	 * @see #FingerprintImage(byte[], FingerprintImageOptions)
	 * @see FingerprintCompatibility#convert(byte[])
	 * @see FingerprintTemplate#FingerprintTemplate(byte[])
	 */
	public FingerprintImage(int width, int height, byte[] pixels, FingerprintImageOptions options) {
		Objects.requireNonNull(pixels);
		if (width <= 0 || height <= 0 || pixels.length != width * height)
			throw new IndexOutOfBoundsException();
		if (options == null)
			options = new FingerprintImageOptions();
		dpi = options.dpi;
		matrix = new DoubleMatrix(width, height);
		for (int y = 0; y < height; ++y)
			for (int x = 0; x < width; ++x)
				matrix.set(x, y, 1 - Byte.toUnsignedInt(pixels[y * width + x]) / 255.0);
	}
	/**
	 * Reads raw grayscale fingerprint image from byte array using default options.
	 * 
	 * @param width
	 *            width of the image
	 * @param height
	 *            height of the image
	 * @param pixels
	 *            image pixels ordered from top-left to bottom-right in horizontal rows
	 * @throws NullPointerException
	 *             if {@code pixels} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code width} or {@code height} is not positive or if {@code pixels} length is not {@code width * height}
	 * 
	 * @see #FingerprintImage(byte[])
	 * @see FingerprintCompatibility#convert(byte[])
	 * @see FingerprintTemplate#FingerprintTemplate(byte[])
	 */
	public FingerprintImage(int width, int height, byte[] pixels) {
		this(width, height, pixels, null);
	}
	/**
	 * @deprecated Use one of the constructors that fully initialize the object.
	 * 
	 * @see #FingerprintImage(byte[], FingerprintImageOptions)
	 * @see #FingerprintImage(int, int, byte[], FingerprintImageOptions)
	 */
	@Deprecated
	public FingerprintImage() {
	}
	/**
	 * @deprecated Set DPI via {@link FingerprintImageOptions#dpi(double)} instead.
	 * 
	 * @param dpi
	 *            DPI of the fingerprint image
	 * @return {@code this} (fluent method)
	 * @throws IllegalArgumentException
	 *             if {@code dpi} is non-positive, impossibly low, or impossibly high
	 * 
	 * @see FingerprintImageOptions#dpi(double)
	 */
	@Deprecated
	public FingerprintImage dpi(double dpi) {
		if (dpi < 20 || dpi > 20_000)
			throw new IllegalArgumentException();
		this.dpi = dpi;
		return this;
	}
	/**
	 * @deprecated Use {@link #FingerprintImage(byte[], FingerprintImageOptions)} constructor to decode image in standard format.
	 * 
	 * @param image
	 *            fingerprint image in one of the supported formats
	 * @return {@code this} (fluent method)
	 * @throws NullPointerException
	 *             if {@code image} is {@code null}
	 * @throws IllegalArgumentException
	 *             if the image format is unsupported or the image is corrupted
	 * 
	 * @see #FingerprintImage(byte[], FingerprintImageOptions)
	 */
	@Deprecated
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
	 * @deprecated Use {@link #FingerprintImage(int, int, byte[], FingerprintImageOptions)} constructor to read raw image.
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
	 * @see #FingerprintImage(int, int, byte[], FingerprintImageOptions)
	 */
	@Deprecated
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
