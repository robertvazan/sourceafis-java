// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.images;

import com.machinezoo.sourceafis.engine.configuration.*;

/*
 * This decoder uses Android's Bitmap class to decode templates.
 * Note that Bitmap class will not work in unit tests. It only works inside a full-blown emulator.
 * 
 * Since direct references of Android libraries would not compile,
 * we will reference BitmapFactory and Bitmap via reflection.
 */
class AndroidImageDecoder extends ImageDecoder {
	@Override
	public boolean available() {
		return PlatformCheck.hasClass("android.graphics.BitmapFactory");
	}
	@Override
	public String name() {
		return "Android";
	}
	@Override
	public DecodedImage decode(byte[] image) {
		AndroidBitmap bitmap = AndroidBitmapFactory.decodeByteArray(image, 0, image.length);
		if (bitmap.instance == null)
			throw new IllegalArgumentException("Unsupported image format.");
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int[] pixels = new int[width * height];
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
		return new DecodedImage(width, height, pixels);
	}
}
