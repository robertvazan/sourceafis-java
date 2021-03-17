// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static java.util.stream.Collectors.*;
import java.util.*;
import java.util.stream.*;
import com.machinezoo.fingerprintio.*;
import com.machinezoo.fingerprintio.ansi378v2004.*;
import com.machinezoo.fingerprintio.ansi378v2009.*;
import com.machinezoo.fingerprintio.ansi378v2009am1.*;
import com.machinezoo.fingerprintio.iso19794p2v2005.*;
import com.machinezoo.fingerprintio.iso19794p2v2011.*;

abstract class TemplateCodec {
	abstract byte[] encode(List<MutableTemplate> templates);
	abstract List<MutableTemplate> decode(byte[] serialized, boolean strict);
	List<MutableTemplate> decode(byte[] serialized) {
		try {
			return decode(serialized, true);
		} catch (Throwable ex) {
			return decode(serialized, false);
		}
	}
	static final Map<TemplateFormat, TemplateCodec> ALL = new HashMap<>();
	static {
		ALL.put(TemplateFormat.ANSI_378_2004, new Ansi378v2004Codec());
		ALL.put(TemplateFormat.ANSI_378_2009, new Ansi378v2009Codec());
		ALL.put(TemplateFormat.ANSI_378_2009_AM1, new Ansi378v2009Am1Codec());
		ALL.put(TemplateFormat.ISO_19794_2_2005, new Iso19794p2v2005Codec());
		ALL.put(TemplateFormat.ISO_19794_2_2011, new Iso19794p2v2011Codec());
	}
	static class Resolution {
		double dpiX;
		double dpiY;
	}
	static IntPoint decode(int x, int y, Resolution resolution) {
		return new IntPoint(decode(x, resolution.dpiX), decode(y, resolution.dpiY));
	}
	static int decode(int value, double dpi) {
		return (int)Math.round(value / dpi * 500);
	}
	private static class Ansi378v2004Codec extends TemplateCodec {
		@Override byte[] encode(List<MutableTemplate> templates) {
			int resolution = (int)Math.round(500 / 2.54);
			Ansi378v2004Template iotemplate = new Ansi378v2004Template();
			iotemplate.width = templates.stream().mapToInt(t -> t.size.x).max().orElse(500);
			iotemplate.height = templates.stream().mapToInt(t -> t.size.y).max().orElse(500);
			iotemplate.resolutionX = resolution;
			iotemplate.resolutionY = resolution;
			iotemplate.fingerprints = IntStream.range(0, templates.size())
				.mapToObj(n -> encode(n, templates.get(n)))
				.collect(toList());
			return iotemplate.toByteArray();
		}
		@Override List<MutableTemplate> decode(byte[] serialized, boolean strict) {
			Ansi378v2004Template iotemplate = new Ansi378v2004Template(serialized, strict);
			Resolution resolution = new Resolution();
			resolution.dpiX = iotemplate.resolutionX * 2.54;
			resolution.dpiY = iotemplate.resolutionY * 2.54;
			return iotemplate.fingerprints.stream()
				.map(fp -> decode(fp, iotemplate, resolution))
				.collect(toList());
		}
		static Ansi378v2004Fingerprint encode(int offset, MutableTemplate template) {
			Ansi378v2004Fingerprint iofingerprint = new Ansi378v2004Fingerprint();
			iofingerprint.view = offset;
			iofingerprint.minutiae = template.minutiae.stream()
				.map(m -> encode(m))
				.collect(toList());
			return iofingerprint;
		}
		static MutableTemplate decode(Ansi378v2004Fingerprint iofingerprint, Ansi378v2004Template iotemplate, Resolution resolution) {
			MutableTemplate template = new MutableTemplate();
			template.size = decode(iotemplate.width, iotemplate.height, resolution);
			template.minutiae = iofingerprint.minutiae.stream()
				.map(m -> decode(m, resolution))
				.collect(toList());
			return template;
		}
		static Ansi378v2004Minutia encode(MutableMinutia minutia) {
			Ansi378v2004Minutia iominutia = new Ansi378v2004Minutia();
			iominutia.positionX = minutia.position.x;
			iominutia.positionY = minutia.position.y;
			iominutia.angle = encodeAngle(minutia.direction);
			iominutia.type = encode(minutia.type);
			return iominutia;
		}
		static MutableMinutia decode(Ansi378v2004Minutia iominutia, Resolution resolution) {
			MutableMinutia minutia = new MutableMinutia();
			minutia.position = decode(iominutia.positionX, iominutia.positionY, resolution);
			minutia.direction = decodeAngle(iominutia.angle);
			minutia.type = decode(iominutia.type);
			return minutia;
		}
		static int encodeAngle(double angle) {
			return (int)Math.ceil(DoubleAngle.complementary(angle) * DoubleAngle.INV_PI2 * 360 / 2) % 180;
		}
		static double decodeAngle(int ioangle) {
			return DoubleAngle.complementary(((2 * ioangle - 1 + 360) % 360) / 360.0 * DoubleAngle.PI2);
		}
		static Ansi378v2004MinutiaType encode(MinutiaType type) {
			switch (type) {
			case ENDING:
				return Ansi378v2004MinutiaType.ENDING;
			case BIFURCATION:
				return Ansi378v2004MinutiaType.BIFURCATION;
			default:
				return Ansi378v2004MinutiaType.ENDING;
			}
		}
		static MinutiaType decode(Ansi378v2004MinutiaType iotype) {
			switch (iotype) {
			case ENDING:
				return MinutiaType.ENDING;
			case BIFURCATION:
				return MinutiaType.BIFURCATION;
			default:
				return MinutiaType.ENDING;
			}
		}
	}
	private static class Ansi378v2009Codec extends TemplateCodec {
		@Override byte[] encode(List<MutableTemplate> templates) {
			Ansi378v2009Template iotemplate = new Ansi378v2009Template();
			iotemplate.fingerprints = IntStream.range(0, templates.size())
				.mapToObj(n -> encode(n, templates.get(n)))
				.collect(toList());
			return iotemplate.toByteArray();
		}
		@Override List<MutableTemplate> decode(byte[] serialized, boolean strict) {
			return new Ansi378v2009Template(serialized, strict).fingerprints.stream()
				.map(fp -> decode(fp))
				.collect(toList());
		}
		static Ansi378v2009Fingerprint encode(int offset, MutableTemplate template) {
			int resolution = (int)Math.round(500 / 2.54);
			Ansi378v2009Fingerprint iofingerprint = new Ansi378v2009Fingerprint();
			iofingerprint.view = offset;
			iofingerprint.width = template.size.x;
			iofingerprint.height = template.size.y;
			iofingerprint.resolutionX = resolution;
			iofingerprint.resolutionY = resolution;
			iofingerprint.minutiae = template.minutiae.stream()
				.map(m -> encode(m))
				.collect(toList());
			return iofingerprint;
		}
		static MutableTemplate decode(Ansi378v2009Fingerprint iofingerprint) {
			Resolution resolution = new Resolution();
			resolution.dpiX = iofingerprint.resolutionX * 2.54;
			resolution.dpiY = iofingerprint.resolutionY * 2.54;
			MutableTemplate template = new MutableTemplate();
			template.size = decode(iofingerprint.width, iofingerprint.height, resolution);
			template.minutiae = iofingerprint.minutiae.stream()
				.map(m -> decode(m, resolution))
				.collect(toList());
			return template;
		}
		static Ansi378v2009Minutia encode(MutableMinutia minutia) {
			Ansi378v2009Minutia iominutia = new Ansi378v2009Minutia();
			iominutia.positionX = minutia.position.x;
			iominutia.positionY = minutia.position.y;
			iominutia.angle = encodeAngle(minutia.direction);
			iominutia.type = encode(minutia.type);
			return iominutia;
		}
		static MutableMinutia decode(Ansi378v2009Minutia iominutia, Resolution resolution) {
			MutableMinutia minutia = new MutableMinutia();
			minutia.position = decode(iominutia.positionX, iominutia.positionY, resolution);
			minutia.direction = decodeAngle(iominutia.angle);
			minutia.type = decode(iominutia.type);
			return minutia;
		}
		static int encodeAngle(double angle) {
			return (int)Math.ceil(DoubleAngle.complementary(angle) * DoubleAngle.INV_PI2 * 360 / 2) % 180;
		}
		static double decodeAngle(int ioangle) {
			return DoubleAngle.complementary(((2 * ioangle - 1 + 360) % 360) / 360.0 * DoubleAngle.PI2);
		}
		static Ansi378v2009MinutiaType encode(MinutiaType type) {
			switch (type) {
			case ENDING:
				return Ansi378v2009MinutiaType.ENDING;
			case BIFURCATION:
				return Ansi378v2009MinutiaType.BIFURCATION;
			default:
				return Ansi378v2009MinutiaType.ENDING;
			}
		}
		static MinutiaType decode(Ansi378v2009MinutiaType iotype) {
			switch (iotype) {
			case ENDING:
				return MinutiaType.ENDING;
			case BIFURCATION:
				return MinutiaType.BIFURCATION;
			default:
				return MinutiaType.ENDING;
			}
		}
	}
	private static class Ansi378v2009Am1Codec extends TemplateCodec {
		@Override byte[] encode(List<MutableTemplate> templates) {
			Ansi378v2009Am1Template iotemplate = new Ansi378v2009Am1Template();
			iotemplate.fingerprints = IntStream.range(0, templates.size())
				.mapToObj(n -> encode(n, templates.get(n)))
				.collect(toList());
			return iotemplate.toByteArray();
		}
		@Override List<MutableTemplate> decode(byte[] serialized, boolean strict) {
			return new Ansi378v2009Am1Template(serialized, strict).fingerprints.stream()
				.map(fp -> decode(fp))
				.collect(toList());
		}
		static Ansi378v2009Am1Fingerprint encode(int offset, MutableTemplate template) {
			int resolution = (int)Math.round(500 / 2.54);
			Ansi378v2009Am1Fingerprint iofingerprint = new Ansi378v2009Am1Fingerprint();
			iofingerprint.view = offset;
			iofingerprint.width = template.size.x;
			iofingerprint.height = template.size.y;
			iofingerprint.resolutionX = resolution;
			iofingerprint.resolutionY = resolution;
			iofingerprint.minutiae = template.minutiae.stream()
				.map(m -> encode(m))
				.collect(toList());
			return iofingerprint;
		}
		static MutableTemplate decode(Ansi378v2009Am1Fingerprint iofingerprint) {
			Resolution resolution = new Resolution();
			resolution.dpiX = iofingerprint.resolutionX * 2.54;
			resolution.dpiY = iofingerprint.resolutionY * 2.54;
			MutableTemplate template = new MutableTemplate();
			template.size = decode(iofingerprint.width, iofingerprint.height, resolution);
			template.minutiae = iofingerprint.minutiae.stream()
				.map(m -> decode(m, resolution))
				.collect(toList());
			return template;
		}
		static Ansi378v2009Am1Minutia encode(MutableMinutia minutia) {
			Ansi378v2009Am1Minutia iominutia = new Ansi378v2009Am1Minutia();
			iominutia.positionX = minutia.position.x;
			iominutia.positionY = minutia.position.y;
			iominutia.angle = encodeAngle(minutia.direction);
			iominutia.type = encode(minutia.type);
			return iominutia;
		}
		static MutableMinutia decode(Ansi378v2009Am1Minutia iominutia, Resolution resolution) {
			MutableMinutia minutia = new MutableMinutia();
			minutia.position = decode(iominutia.positionX, iominutia.positionY, resolution);
			minutia.direction = decodeAngle(iominutia.angle);
			minutia.type = decode(iominutia.type);
			return minutia;
		}
		static int encodeAngle(double angle) {
			return (int)Math.ceil(DoubleAngle.complementary(angle) * DoubleAngle.INV_PI2 * 360 / 2) % 180;
		}
		static double decodeAngle(int ioangle) {
			return DoubleAngle.complementary(((2 * ioangle - 1 + 360) % 360) / 360.0 * DoubleAngle.PI2);
		}
		static Ansi378v2009Am1MinutiaType encode(MinutiaType type) {
			switch (type) {
			case ENDING:
				return Ansi378v2009Am1MinutiaType.ENDING;
			case BIFURCATION:
				return Ansi378v2009Am1MinutiaType.BIFURCATION;
			default:
				return Ansi378v2009Am1MinutiaType.ENDING;
			}
		}
		static MinutiaType decode(Ansi378v2009Am1MinutiaType iotype) {
			switch (iotype) {
			case ENDING:
				return MinutiaType.ENDING;
			case BIFURCATION:
				return MinutiaType.BIFURCATION;
			default:
				return MinutiaType.ENDING;
			}
		}
	}
	private static class Iso19794p2v2005Codec extends TemplateCodec {
		@Override byte[] encode(List<MutableTemplate> templates) {
			int resolution = (int)Math.round(500 / 2.54);
			Iso19794p2v2005Template iotemplate = new Iso19794p2v2005Template();
			iotemplate.width = templates.stream().mapToInt(t -> t.size.x).max().orElse(500);
			iotemplate.height = templates.stream().mapToInt(t -> t.size.y).max().orElse(500);
			iotemplate.resolutionX = resolution;
			iotemplate.resolutionY = resolution;
			iotemplate.fingerprints = IntStream.range(0, templates.size())
				.mapToObj(n -> encode(n, templates.get(n)))
				.collect(toList());
			return iotemplate.toByteArray();
		}
		@Override List<MutableTemplate> decode(byte[] serialized, boolean strict) {
			Iso19794p2v2005Template iotemplate = new Iso19794p2v2005Template(serialized, strict);
			Resolution resolution = new Resolution();
			resolution.dpiX = iotemplate.resolutionX * 2.54;
			resolution.dpiY = iotemplate.resolutionY * 2.54;
			return iotemplate.fingerprints.stream()
				.map(fp -> decode(fp, iotemplate, resolution))
				.collect(toList());
		}
		static Iso19794p2v2005Fingerprint encode(int offset, MutableTemplate template) {
			Iso19794p2v2005Fingerprint iofingerprint = new Iso19794p2v2005Fingerprint();
			iofingerprint.view = offset;
			iofingerprint.minutiae = template.minutiae.stream()
				.map(m -> encode(m))
				.collect(toList());
			return iofingerprint;
		}
		static MutableTemplate decode(Iso19794p2v2005Fingerprint iofingerprint, Iso19794p2v2005Template iotemplate, Resolution resolution) {
			MutableTemplate template = new MutableTemplate();
			template.size = decode(iotemplate.width, iotemplate.height, resolution);
			template.minutiae = iofingerprint.minutiae.stream()
				.map(m -> decode(m, resolution))
				.collect(toList());
			return template;
		}
		static Iso19794p2v2005Minutia encode(MutableMinutia minutia) {
			Iso19794p2v2005Minutia iominutia = new Iso19794p2v2005Minutia();
			iominutia.positionX = minutia.position.x;
			iominutia.positionY = minutia.position.y;
			iominutia.angle = encodeAngle(minutia.direction);
			iominutia.type = encode(minutia.type);
			return iominutia;
		}
		static MutableMinutia decode(Iso19794p2v2005Minutia iominutia, Resolution resolution) {
			MutableMinutia minutia = new MutableMinutia();
			minutia.position = decode(iominutia.positionX, iominutia.positionY, resolution);
			minutia.direction = decodeAngle(iominutia.angle);
			minutia.type = decode(iominutia.type);
			return minutia;
		}
		static int encodeAngle(double angle) {
			return (int)Math.round(DoubleAngle.complementary(angle) * DoubleAngle.INV_PI2 * 256) & 0xff;
		}
		static double decodeAngle(int ioangle) {
			return DoubleAngle.complementary(ioangle / 256.0 * DoubleAngle.PI2);
		}
		static Iso19794p2v2005MinutiaType encode(MinutiaType type) {
			switch (type) {
			case ENDING:
				return Iso19794p2v2005MinutiaType.ENDING;
			case BIFURCATION:
				return Iso19794p2v2005MinutiaType.BIFURCATION;
			default:
				return Iso19794p2v2005MinutiaType.ENDING;
			}
		}
		static MinutiaType decode(Iso19794p2v2005MinutiaType iotype) {
			switch (iotype) {
			case ENDING:
				return MinutiaType.ENDING;
			case BIFURCATION:
				return MinutiaType.BIFURCATION;
			default:
				return MinutiaType.ENDING;
			}
		}
	}
	private static class Iso19794p2v2011Codec extends TemplateCodec {
		@Override byte[] encode(List<MutableTemplate> templates) {
			Iso19794p2v2011Template iotemplate = new Iso19794p2v2011Template();
			iotemplate.fingerprints = IntStream.range(0, templates.size())
				.mapToObj(n -> encode(n, templates.get(n)))
				.collect(toList());
			return iotemplate.toByteArray();
		}
		@Override List<MutableTemplate> decode(byte[] serialized, boolean strict) {
			Iso19794p2v2011Template iotemplate = new Iso19794p2v2011Template(serialized, strict);
			return iotemplate.fingerprints.stream()
				.map(fp -> decode(fp))
				.collect(toList());
		}
		static Iso19794p2v2011Fingerprint encode(int offset, MutableTemplate template) {
			int resolution = (int)Math.round(500 / 2.54);
			Iso19794p2v2011Fingerprint iofingerprint = new Iso19794p2v2011Fingerprint();
			iofingerprint.view = offset;
			iofingerprint.width = template.size.x;
			iofingerprint.height = template.size.y;
			iofingerprint.resolutionX = resolution;
			iofingerprint.resolutionY = resolution;
			iofingerprint.endingType = Iso19794p2v2011EndingType.RIDGE_SKELETON_ENDPOINT;
			iofingerprint.minutiae = template.minutiae.stream()
				.map(m -> encode(m))
				.collect(toList());
			return iofingerprint;
		}
		static MutableTemplate decode(Iso19794p2v2011Fingerprint iofingerprint) {
			Resolution resolution = new Resolution();
			resolution.dpiX = iofingerprint.resolutionX * 2.54;
			resolution.dpiY = iofingerprint.resolutionY * 2.54;
			MutableTemplate template = new MutableTemplate();
			template.size = decode(iofingerprint.width, iofingerprint.height, resolution);
			template.minutiae = iofingerprint.minutiae.stream()
				.map(m -> decode(m, resolution))
				.collect(toList());
			return template;
		}
		static Iso19794p2v2011Minutia encode(MutableMinutia minutia) {
			Iso19794p2v2011Minutia iominutia = new Iso19794p2v2011Minutia();
			iominutia.positionX = minutia.position.x;
			iominutia.positionY = minutia.position.y;
			iominutia.angle = encodeAngle(minutia.direction);
			iominutia.type = encode(minutia.type);
			return iominutia;
		}
		static MutableMinutia decode(Iso19794p2v2011Minutia iominutia, Resolution resolution) {
			MutableMinutia minutia = new MutableMinutia();
			minutia.position = decode(iominutia.positionX, iominutia.positionY, resolution);
			minutia.direction = decodeAngle(iominutia.angle);
			minutia.type = decode(iominutia.type);
			return minutia;
		}
		static int encodeAngle(double angle) {
			return (int)Math.round(DoubleAngle.complementary(angle) * DoubleAngle.INV_PI2 * 256) & 0xff;
		}
		static double decodeAngle(int ioangle) {
			return DoubleAngle.complementary(ioangle / 256.0 * DoubleAngle.PI2);
		}
		static Iso19794p2v2011MinutiaType encode(MinutiaType type) {
			switch (type) {
			case ENDING:
				return Iso19794p2v2011MinutiaType.ENDING;
			case BIFURCATION:
				return Iso19794p2v2011MinutiaType.BIFURCATION;
			default:
				return Iso19794p2v2011MinutiaType.ENDING;
			}
		}
		static MinutiaType decode(Iso19794p2v2011MinutiaType iotype) {
			switch (iotype) {
			case ENDING:
				return MinutiaType.ENDING;
			case BIFURCATION:
				return MinutiaType.BIFURCATION;
			default:
				return MinutiaType.ENDING;
			}
		}
	}
}
