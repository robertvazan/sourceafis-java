// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.io.*;
import org.slf4j.*;

class ForeignDimensions {
	private static final Logger logger = LoggerFactory.getLogger(ForeignDimensions.class);
	int width;
	int height;
	double dpiX;
	double dpiY;
	ForeignDimensions(FingerprintTemplate template) {
		width = template.immutable.size.x;
		height = template.immutable.size.y;
		dpiX = 500;
		dpiY = 500;
	}
	ForeignDimensions(DataInputStream in) throws IOException {
		width = in.readUnsignedShort();
		height = in.readUnsignedShort();
		if (width == 0 || height == 0)
			logger.warn("Bad template: zero image width or height");
		dpiX = in.readUnsignedShort() * 2.54;
		dpiY = in.readUnsignedShort() * 2.54;
		if (dpiX < 5 || dpiY < 5)
			throw new IllegalArgumentException("Bad template: zero or too low DPI");
		if (dpiX < 200 || dpiX > 2000 || dpiY < 200 || dpiY > 2000)
			logger.warn("Probably bad template: DPI lower than 200 or higher than 2,000");
	}
	void write(DataOutputStream out) throws IOException {
		if (width < 0 || width >= 0x10000 || height < 0 || height >= 0x10000)
			throw new IllegalArgumentException("Cannot create template: image dimensions are not 16-bit unsigned numbers");
		if (width == 0 || height == 0)
			throw new IllegalArgumentException("Cannot create template: zero image width or height");
		out.writeShort(width);
		out.writeShort(height);
		double dpiMax = 0xffff * 2.54;
		if (dpiX < 0 || dpiX > dpiMax || dpiY < 0 || dpiY > dpiMax)
			throw new IllegalArgumentException("Cannot create template: DPI cannot be encoded as a 16-bit unsigned number in px/cm units");
		if (dpiX < 5 || dpiY < 5)
			throw new IllegalArgumentException("Cannot create template: zero or too low DPI");
		if (dpiX < 200 || dpiX > 2000 || dpiY < 200 || dpiY > 2000)
			logger.warn("Creating probably bad template: DPI lower than 200 or higher than 2,000");
		out.writeShort((int)Math.round(dpiX / 2.54));
		out.writeShort((int)Math.round(dpiY / 2.54));
	}
}
