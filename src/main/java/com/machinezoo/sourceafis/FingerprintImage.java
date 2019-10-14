// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import javax.imageio.*;

/**
 * Pixels and metadata of the fingerprint image.
 * This class captures all fingerprint information that is available prior to construction of {@link FingerprintTemplate}.
 * Since SourceAFIS algorithm is not scale-invariant, all images should have DPI configured explicitly by calling {@link #dpi(double)}.
 * <p>
 * Application should start fingerprint processing by constructing an instance of {@code FingerprintImage}
 * and then passing it to {@link FingerprintTemplate#FingerprintTemplate(FingerprintImage)}.
 * 
 * @see FingerprintTemplate
 */
public class FingerprintImage {
	double dpi = 500;
	/**
	 * Set DPI (dots per inch) of the fingerprint image.
	 * This is the DPI of the image later passed to {@link #decode(byte[])}.
	 * Check your fingerprint reader specification for correct DPI value. Default DPI is 500.
	 * 
	 * @param dpi
	 *            DPI of the fingerprint image, usually around 500
	 * @return {@code this} (fluent method)
	 * 
	 * @see #decode(byte[])
	 */
	public FingerprintImage dpi(double dpi) {
		this.dpi = dpi;
		return this;
	}
	DoubleMap decoded;
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
	 * 
	 * @see #dpi(double)
	 */
	public FingerprintImage decode(byte[] image) {
		decoded = ImageDecoder.toDoubleMap(image);
		return this;
	}
}
