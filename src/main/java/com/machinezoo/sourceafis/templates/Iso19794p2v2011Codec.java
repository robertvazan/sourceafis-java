// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.templates;

import static java.util.stream.Collectors.*;
import java.util.*;
import java.util.stream.*;
import com.machinezoo.fingerprintio.iso19794p2v2011.*;
import com.machinezoo.sourceafis.features.*;
import com.machinezoo.sourceafis.primitives.*;

class Iso19794p2v2011Codec extends TemplateCodec {
	@Override
	public byte[] encode(List<MutableTemplate> templates) {
		Iso19794p2v2011Template iotemplate = new Iso19794p2v2011Template();
		iotemplate.fingerprints = IntStream.range(0, templates.size())
			.mapToObj(n -> encode(n, templates.get(n)))
			.collect(toList());
		return iotemplate.toByteArray();
	}
	@Override
	public List<MutableTemplate> decode(byte[] serialized, boolean strict) {
		Iso19794p2v2011Template iotemplate = new Iso19794p2v2011Template(serialized, strict);
		return iotemplate.fingerprints.stream()
			.map(fp -> decode(fp))
			.collect(toList());
	}
	private static Iso19794p2v2011Fingerprint encode(int offset, MutableTemplate template) {
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
	private static MutableTemplate decode(Iso19794p2v2011Fingerprint iofingerprint) {
		TemplateResolution resolution = new TemplateResolution();
		resolution.dpiX = iofingerprint.resolutionX * 2.54;
		resolution.dpiY = iofingerprint.resolutionY * 2.54;
		MutableTemplate template = new MutableTemplate();
		template.size = resolution.decode(iofingerprint.width, iofingerprint.height);
		template.minutiae = iofingerprint.minutiae.stream()
			.map(m -> decode(m, resolution))
			.collect(toList());
		return template;
	}
	private static Iso19794p2v2011Minutia encode(MutableMinutia minutia) {
		Iso19794p2v2011Minutia iominutia = new Iso19794p2v2011Minutia();
		iominutia.positionX = minutia.position.x;
		iominutia.positionY = minutia.position.y;
		iominutia.angle = encodeAngle(minutia.direction);
		iominutia.type = encode(minutia.type);
		return iominutia;
	}
	private static MutableMinutia decode(Iso19794p2v2011Minutia iominutia, TemplateResolution resolution) {
		MutableMinutia minutia = new MutableMinutia();
		minutia.position = resolution.decode(iominutia.positionX, iominutia.positionY);
		minutia.direction = decodeAngle(iominutia.angle);
		minutia.type = decode(iominutia.type);
		return minutia;
	}
	private static int encodeAngle(double angle) {
		return (int)Math.round(DoubleAngle.complementary(angle) * DoubleAngle.INV_PI2 * 256) & 0xff;
	}
	private static double decodeAngle(int ioangle) {
		return DoubleAngle.complementary(ioangle / 256.0 * DoubleAngle.PI2);
	}
	private static Iso19794p2v2011MinutiaType encode(MinutiaType type) {
		switch (type) {
			case ENDING :
				return Iso19794p2v2011MinutiaType.ENDING;
			case BIFURCATION :
				return Iso19794p2v2011MinutiaType.BIFURCATION;
			default :
				return Iso19794p2v2011MinutiaType.ENDING;
		}
	}
	private static MinutiaType decode(Iso19794p2v2011MinutiaType iotype) {
		switch (iotype) {
			case ENDING :
				return MinutiaType.ENDING;
			case BIFURCATION :
				return MinutiaType.BIFURCATION;
			default :
				return MinutiaType.ENDING;
		}
	}
}
