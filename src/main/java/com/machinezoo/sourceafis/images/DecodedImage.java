// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.images;

public class DecodedImage {
	public int width;
	public int height;
	/*
	 * Format identical to BufferedImage.TYPE_INT_ARGB, i.e. 8 bits for alpha (FF is opaque, 00 is transparent)
	 * followed by 8-bit values for red, green, and blue in this order from highest bits to lowest.
	 */
	public int[] pixels;
	public DecodedImage(int width, int height, int[] pixels) {
		this.width = width;
		this.height = height;
		this.pixels = pixels;
	}
}
