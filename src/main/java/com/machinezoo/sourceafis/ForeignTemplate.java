// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.io.*;
import java.util.*;
import org.slf4j.*;

class ForeignTemplate {
	private static final Logger logger = LoggerFactory.getLogger(ForeignTemplate.class);
	ForeignFormat format;
	ForeignDimensions dimensions;
	List<ForeignFingerprint> fingerprints = new ArrayList<>();
	ForeignTemplate(FingerprintTemplate... templates) {
		for (FingerprintTemplate template : templates)
			fingerprints.add(new ForeignFingerprint(template));
		dimensions = fingerprints.stream().findFirst().map(f -> f.dimensions).orElse(null);
	}
	ForeignTemplate(DataInputStream in) throws IOException {
		readFormatMarker(in);
		readVersion(in);
		readTemplateLength(in);
		if (format == ForeignFormat.ISO_19794_2_2005)
			in.skipBytes(2);
		if (format != ForeignFormat.ISO_19794_2_2005) {
			readProductId(in);
			readSensorInfo(in);
		}
		if (format == ForeignFormat.ANSI_378_2004 || format == ForeignFormat.ISO_19794_2_2005)
			dimensions = new ForeignDimensions(in);
		int count = readFingerprintCount(in);
		in.skipBytes(1);
		for (int i = 0; i < count; ++i)
			fingerprints.add(new ForeignFingerprint(in, format, dimensions));
		if (format == ForeignFormat.ANSI_378_2009 || format == ForeignFormat.ANSI_378_2009_AM1)
			dimensions = fingerprints.stream().findFirst().map(f -> f.dimensions).orElse(null);
	}
	static ForeignTemplate read(byte[] template) {
		try {
			return new ForeignTemplate(new DataInputStream(new ByteArrayInputStream(template)));
		} catch (EOFException ex) {
			throw new IllegalArgumentException("Unexpected end of template data");
		} catch (IOException ex) {
			throw new IllegalStateException("Unexpected I/O error", ex);
		}
	}
	void write(DataOutputStream out) throws IOException {
		if (format == ForeignFormat.ISO_19794_2_2005)
			throw new IllegalStateException();
		writeFormatMarker(out);
		writeVersion(out);
		writeTemplateLength(out);
		writeProductId(out);
		writeSensorInfo(out);
		if (format == ForeignFormat.ANSI_378_2004)
			dimensions.write(out);
		writeFingerprintCount(out);
		out.writeByte(0);
		for (int i = 0; i < fingerprints.size(); ++i)
			fingerprints.get(i).write(out, format, i);
	}
	byte[] write() {
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			write(new DataOutputStream(buffer));
			return buffer.toByteArray();
		} catch (IOException ex) {
			throw new IllegalStateException("Unexpected I/O error", ex);
		}
	}
	private void readFormatMarker(DataInputStream in) throws IOException {
		if (in.readByte() != 'F' || in.readByte() != 'M' || in.readByte() != 'R' || in.readByte() != 0)
			throw new IllegalArgumentException("Unsupported template format: missing FMR signature at the beginning");
	}
	private void writeFormatMarker(DataOutputStream out) throws IOException {
		out.writeByte('F');
		out.writeByte('M');
		out.writeByte('R');
		out.writeByte(0);
	}
	private void readVersion(DataInputStream in) throws IOException {
		byte v0 = in.readByte();
		byte v1 = in.readByte();
		byte v2 = in.readByte();
		byte v3 = in.readByte();
		if (v0 == ' ' && v1 == '2' && v2 == '0' && v3 == 0) {
			// temporary value, differentiate between ISO and ANSI using length field 
			format = ForeignFormat.ANSI_378_2004;
		} else if (v0 == '0' && v1 == '3' && v2 == '0' && v3 == 0)
			format = ForeignFormat.ANSI_378_2009;
		else if (v0 == '0' && v1 == '3' && v2 == '5' && v3 == 0)
			format = ForeignFormat.ANSI_378_2009_AM1;
		else
			throw new IllegalArgumentException("Unsupported template version: must be one of 20, 030, or 035");
	}
	private void writeVersion(DataOutputStream out) throws IOException {
		switch (format) {
		case ANSI_378_2004:
			out.writeByte(' ');
			out.writeByte('2');
			out.writeByte('0');
			out.writeByte(0);
			break;
		case ANSI_378_2009:
			out.writeByte('0');
			out.writeByte('3');
			out.writeByte('0');
			out.writeByte(0);
			break;
		case ANSI_378_2009_AM1:
			out.writeByte('0');
			out.writeByte('3');
			out.writeByte('5');
			out.writeByte(0);
			break;
		default:
			throw new IllegalStateException();
		}
	}
	private void readTemplateLength(DataInputStream in) throws IOException {
		if (format == ForeignFormat.ANSI_378_2004) {
			int bytes01 = in.readUnsignedShort();
			if (bytes01 >= 26) {
				// too big for ISO 19794-2, it's indeed ANSI 378-2004, 2-byte length field
			} else if (bytes01 > 0) {
				// invalid length field for ANSI 378, must be ISO 19794-2
				format = ForeignFormat.ISO_19794_2_2005;
				int length = (bytes01 << 16) | in.readUnsignedShort();
				if (length < 24)
					logger.warn("Bad template: total length must be at least 24 bytes");
			} else {
				int bytes23 = in.readUnsignedShort();
				if (bytes23 >= 24) {
					// too big for ANSI 378, must be ISO 19794-2
					format = ForeignFormat.ISO_19794_2_2005;
				} else {
					// it's ANSI 378-2004 after all, 6-byte length field
					int length = (bytes23 << 16) | in.readUnsignedShort();
					if (length < 26)
						logger.warn("Bad template: total length must be at least 26 bytes");
					else if (length < 0x10000)
						logger.debug("Not strictly compliant template: 6-byte length field should have value of at least 0x10000");
				}
			}
		} else {
			long length = 0xffff_ffffL & in.readInt();
			if (format == ForeignFormat.ANSI_378_2009 && length < 21)
				logger.warn("Bad template: total length must be at least 21 bytes");
			if (format == ForeignFormat.ANSI_378_2009_AM1 && length < 40)
				logger.warn("Bad template: total length must be at least 40 bytes");
		}
	}
	private void writeTemplateLength(DataOutputStream out) throws IOException {
		int total = measure();
		if (format != ForeignFormat.ANSI_378_2004)
			out.writeInt(total);
		else if (total < 0x10000)
			out.writeShort(total);
		else {
			out.writeShort(0);
			out.writeInt(total);
		}
	}
	private int measure() {
		int fixed;
		switch (format) {
		case ANSI_378_2004:
			fixed = 26;
			break;
		case ANSI_378_2009:
		case ANSI_378_2009_AM1:
			fixed = 21;
			break;
		default:
			throw new IllegalArgumentException();
		}
		int total = fixed + fingerprints.stream().mapToInt(f -> f.measure(format)).sum();
		if (format == ForeignFormat.ANSI_378_2004) {
			// enlarge by 4 bytes if we don't fit in the 2-byte length field
			return total < 0x10000 ? total : total + 4;
		} else
			return total;
	}
	private void readProductId(DataInputStream in) throws IOException {
		int vendor = in.readUnsignedShort();
		if (vendor == 0)
			logger.debug("Not strictly compliant template: zero vendor ID / product owner");
		in.skipBytes(2);
	}
	private void writeProductId(DataOutputStream out) throws IOException {
		out.writeShort(0x103); // Vendor Unknown
		out.writeShort(0);
	}
	private void readSensorInfo(DataInputStream in) throws IOException {
		if (format == ForeignFormat.ANSI_378_2004) {
			int combined = in.readUnsignedShort();
			int compliance = combined >> 12;
			if ((compliance & 7) != 0)
				logger.warn("Bad template: reserved bit in sensor compliance field is set");
		} else {
			int compliance = in.readUnsignedByte();
			if ((compliance & 0x7f) != 0)
				logger.warn("Bad template: reserved bit in sensor compliance field is set");
			in.skipBytes(2);
		}
	}
	private void writeSensorInfo(DataOutputStream out) throws IOException {
		if (format == ForeignFormat.ANSI_378_2004)
			out.writeShort(0);
		else {
			out.writeByte(0);
			out.writeShort(0);
		}
	}
	private int readFingerprintCount(DataInputStream in) throws IOException {
		int count = in.readUnsignedByte();
		if ((format == ForeignFormat.ANSI_378_2004 || format == ForeignFormat.ANSI_378_2009) && count > 176)
			logger.warn("Bad template: more than 176 fingerprints");
		if (format == ForeignFormat.ANSI_378_2009_AM1 && count == 0)
			logger.warn("Bad template: zero fingerprint count");
		return count;
	}
	private void writeFingerprintCount(DataOutputStream out) throws IOException {
		if (fingerprints.size() > 16)
			throw new IllegalArgumentException("Cannot create template: more than 16 fingerprints, cannot assign view offsets");
		if (format == ForeignFormat.ANSI_378_2009_AM1 && fingerprints.isEmpty())
			throw new IllegalArgumentException("Cannot create template: no fingerprints");
		out.writeByte(fingerprints.size());
	}
}
