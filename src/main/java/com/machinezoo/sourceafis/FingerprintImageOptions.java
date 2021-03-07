// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

/**
 * Additional information about fingerprint image.
 * {@code FingerprintImageOptions} can be passed to {@link FingerprintImage} constructor
 * to provide additional information about fingerprint image that supplements raw pixel data.
 * Since SourceAFIS algorithm is not scale-invariant, all images should have
 * DPI configured explicitly by calling {@link #dpi(double)}.
 * 
 * @see FingerprintImage
 */
public class FingerprintImageOptions {
	/*
	 * API roadmap:
	 * + position(FingerprintPosition)
	 * + other fingerprint properties
	 */
	double dpi = 500;
	/**
	 * Initializes default options.
	 * Call methods of this class to customize the options.
	 */
	public FingerprintImageOptions() {
	}
	/**
	 * Sets image resolution. Resolution in measured in dots per inch (DPI).
	 * SourceAFIS algorithm is not scale-invariant. Fingerprints with incorrectly configured DPI may fail to match.
	 * Check your fingerprint reader specification for correct DPI value. Default DPI is 500.
	 * 
	 * @param dpi
	 *            image resolution in DPI (dots per inch), usually around 500
	 * @return {@code this} (fluent method)
	 * @throws IllegalArgumentException
	 *             if {@code dpi} is non-positive, impossibly low, or impossibly high
	 */
	public FingerprintImageOptions dpi(double dpi) {
		this.dpi = dpi;
		return this;
	}
}
