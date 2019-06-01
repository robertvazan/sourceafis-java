// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.io.*;
import java.util.*;
import org.slf4j.*;

class ForeignFingerprint {
	private static final Logger logger = LoggerFactory.getLogger(ForeignFingerprint.class);
	ForeignDimensions dimensions;
	List<ForeignMinutia> minutiae = new ArrayList<>();
	ForeignFingerprint(FingerprintTemplate template) {
		dimensions = new ForeignDimensions(template);
		minutiae = Arrays.stream(template.immutable.minutiae).map(ForeignMinutia::new).collect(toList());
	}
	ForeignFingerprint(DataInputStream in, ForeignFormat format, ForeignDimensions sharedDimensions) throws IOException {
		readPosition(in, format);
		if (format == ForeignFormat.ISO_19794_2_2005)
			in.skipBytes(1);
		else
			readOffsetAndImpression(in, format);
		readQuality(in, format);
		if (format == ForeignFormat.ANSI_378_2004 || format == ForeignFormat.ISO_19794_2_2005)
			dimensions = sharedDimensions;
		else
			dimensions = new ForeignDimensions(in);
		int count = readMinutiaCount(in);
		for (int i = 0; i < count; ++i)
			minutiae.add(new ForeignMinutia(in, format, dimensions));
		readExtensions(in);
	}
	void write(DataOutputStream out, ForeignFormat format, int offset) throws IOException {
		writePosition(out);
		writeOffsetAndImpression(out, format, offset);
		writeQuality(out, format);
		if (format != ForeignFormat.ANSI_378_2004)
			dimensions.write(out);
		writeMinutiaCount(out);
		for (ForeignMinutia minutia : minutiae)
			minutia.write(out, format, dimensions);
		writeExtensions(out);
	}
	int measure(ForeignFormat format) {
		int fixed;
		switch (format) {
		case ANSI_378_2004:
			fixed = 6;
			break;
		case ANSI_378_2009:
		case ANSI_378_2009_AM1:
			fixed = 19;
			break;
		default:
			throw new IllegalArgumentException();
		}
		return fixed + 6 * minutiae.size();
	}
	private void readPosition(DataInputStream in, ForeignFormat format) throws IOException {
		int position = in.readUnsignedByte();
		if (format != ForeignFormat.ANSI_378_2009_AM1 && position > 10)
			logger.warn("Bad template: finger position must be in range 0-10");
		if (format == ForeignFormat.ANSI_378_2009_AM1 && !(position <= 10) && !(position >= 13 && position <= 15) && !(position >= 40 && position <= 50))
			logger.warn("Bad template: finger position must be in range 0-10, 13-15, or 40-50");
	}
	private void writePosition(DataOutputStream out) throws IOException {
		out.writeByte(0); // unknown position
	}
	private void readOffsetAndImpression(DataInputStream in, ForeignFormat format) throws IOException {
		if (format == ForeignFormat.ANSI_378_2004) {
			int combined = in.readUnsignedByte();
			int type = combined & 0xf;
			if (!(type <= 3) && type != 8 && type != 9)
				logger.warn("Bad template: sensor category / impression type must be in range 0-3 or 8-9");
		} else {
			int offset = in.readUnsignedByte();
			if (offset > 15)
				logger.warn("Bad template: view offset must be in range 0-15");
			int type = in.readUnsignedByte();
			if (!(type <= 3) && type != 8 && type != 10 && type != 11 && !(type >= 20 && type <= 29))
				logger.warn("Bad template: sensor category / impression type must be in range 0-3, 8, 10-11, or 20-29");
		}
	}
	private void writeOffsetAndImpression(DataOutputStream out, ForeignFormat format, int offset) throws IOException {
		if (offset > 15)
			throw new IllegalArgumentException("Cannot create template: at most 16 views are allowed per finger position");
		if (format == ForeignFormat.ANSI_378_2004)
			out.writeByte(offset << 4); // sensor category = live plain
		else {
			out.writeByte(offset);
			out.writeByte(29); // unknown sensor category / impression type
		}
	}
	private void readQuality(DataInputStream in, ForeignFormat format) throws IOException {
		if (format == ForeignFormat.ISO_19794_2_2005)
			in.skipBytes(1);
		else if (format == ForeignFormat.ANSI_378_2004) {
			int quality = in.readUnsignedByte();
			if (quality > 100)
				logger.warn("Bad template: fingerprint quality must be in range 0-100");
		} else {
			int quality = in.readUnsignedByte();
			if (quality > 100 && quality != 254 && quality != 255)
				logger.warn("Bad template: fingerprint quality must be in range 0-100 or have special value 254 or 255");
			int qowner = in.readUnsignedShort();
			if (qowner == 0)
				logger.debug("Not strictly compliant template: zero quality algorithm owner");
			in.readUnsignedShort();
		}
	}
	private void writeQuality(DataOutputStream out, ForeignFormat format) throws IOException {
		if (format == ForeignFormat.ANSI_378_2004)
			out.writeByte(100);
		else {
			out.writeByte(254);
			out.writeInt(0x01030001);
		}
	}
	private int readMinutiaCount(DataInputStream in) throws IOException {
		return in.readUnsignedByte();
	}
	private void writeMinutiaCount(DataOutputStream out) throws IOException {
		if (minutiae.size() > 255)
			throw new IllegalArgumentException("Cannot create template: maximum number of minutiae is 255");
		out.writeByte(minutiae.size());
	}
	private void readExtensions(DataInputStream in) throws IOException {
		int length = in.readUnsignedShort();
		in.skipBytes(length);
	}
	private void writeExtensions(DataOutputStream out) throws IOException {
		out.writeShort(0);
	}
}
