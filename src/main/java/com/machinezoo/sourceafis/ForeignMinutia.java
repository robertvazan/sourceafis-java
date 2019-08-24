// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.io.*;
import org.slf4j.*;

class ForeignMinutia {
	private static final Logger logger = LoggerFactory.getLogger(ForeignMinutia.class);
	ForeignMinutiaType type;
	int x;
	int y;
	double angle;
	ForeignMinutia(ImmutableMinutia minutia) {
		type = ForeignMinutiaType.convert(minutia.type);
		x = minutia.position.x;
		y = minutia.position.y;
		angle = minutia.direction;
	}
	ForeignMinutia(DataInputStream in, ForeignFormat format, ForeignDimensions dimensions) throws IOException {
		readTypeAndX(in, format, dimensions);
		readY(in, dimensions);
		readAngle(in, format);
		readQuality(in, format);
	}
	void write(DataOutputStream out, ForeignFormat format, ForeignDimensions dimensions) throws IOException {
		writeTypeAndX(out, format, dimensions);
		writeY(out, dimensions);
		writeAngle(out, format);
		writeQuality(out, format);
	}
	private void readTypeAndX(DataInputStream in, ForeignFormat format, ForeignDimensions dimensions) throws IOException {
		int combined = in.readUnsignedShort();
		type = ForeignMinutiaType.decode(combined >> 14, format);
		x = combined & 0x3fff;
		if (x >= dimensions.width)
			logger.warn("Bad template: minutia X position must be within image dimensions");
	}
	private void writeTypeAndX(DataOutputStream out, ForeignFormat format, ForeignDimensions dimensions) throws IOException {
		if (x < 0 || x >= dimensions.width)
			throw new IllegalArgumentException("Cannot create template: minutia X position outside image dimensions");
		if (x >= 0x4000)
			throw new IllegalArgumentException("Cannot create template: minutia X position must be an unsigned 14-bit number");
		out.writeShort((type.encode(format) << 14) | x);
	}
	private void readY(DataInputStream in, ForeignDimensions dimensions) throws IOException {
		int combined = in.readUnsignedShort();
		if (combined >= 0x4000)
			logger.warn("Bad template: top two bits in minutia Y position must be zero");
		y = combined & 0x3fff;
		if (y >= dimensions.height)
			logger.warn("Bad template: minutia Y position must be within image dimensions");
	}
	private void writeY(DataOutputStream out, ForeignDimensions dimensions) throws IOException {
		if (y < 0 || y >= dimensions.height)
			throw new IllegalArgumentException("Cannot create template: minutia Y position outside image dimensions");
		if (y >= 0x4000)
			throw new IllegalArgumentException("Cannot create template: minutia Y position must be an unsigned 14-bit number");
		out.writeShort(y);
	}
	private void readAngle(DataInputStream in, ForeignFormat format) throws IOException {
		int quantized = in.readUnsignedByte();
		if (format == ForeignFormat.ISO_19794_2_2005)
			angle = DoubleAngle.complementary((quantized + 0.5) / 256 * DoubleAngle.PI2);
		else {
			if (quantized >= 180)
				logger.warn("Bad template: minutia angle must be in range 0-179");
			angle = DoubleAngle.complementary(((2 * quantized - 1 + 360) % 360) / 360.0 * DoubleAngle.PI2);
		}
	}
	private void writeAngle(DataOutputStream out, ForeignFormat format) throws IOException {
		double normalized = DoubleAngle.complementary(angle < 0 ? angle + DoubleAngle.PI2 : angle >= DoubleAngle.PI2 ? angle - DoubleAngle.PI2 : angle);
		if (normalized < 0 || normalized >= DoubleAngle.PI2)
			throw new IllegalArgumentException("Cannot create template: angle must be in range [0, 2pi)");
		int quantized = (int)Math.ceil(normalized / DoubleAngle.PI2 * 360 / 2);
		if (quantized >= 180)
			quantized -= 180;
		if (quantized < 0 || quantized >= 180)
			throw new IllegalArgumentException("Cannot create template: angle must be in range 0-179");
		out.writeByte(quantized);
	}
	private void readQuality(DataInputStream in, ForeignFormat format) throws IOException {
		int quality = in.readUnsignedByte();
		if (format == ForeignFormat.ANSI_378_2004) {
			if (quality > 100)
				logger.warn("Bad template: minutia quality must be in range 1-100 or zero");
		} else if (format != ForeignFormat.ISO_19794_2_2005) {
			if (quality > 100 && quality < 254)
				logger.warn("Bad template: minutia quality must be in range 0-100 or a special value 254 or 255");
		}
	}
	private void writeQuality(DataOutputStream out, ForeignFormat format) throws IOException {
		if (format == ForeignFormat.ANSI_378_2004)
			out.writeByte(0);
		else
			out.writeByte(254);
	}
}
