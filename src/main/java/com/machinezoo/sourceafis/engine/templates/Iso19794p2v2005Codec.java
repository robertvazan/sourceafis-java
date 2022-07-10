// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.templates;

import static java.util.stream.Collectors.*;
import java.util.*;
import java.util.stream.*;
import com.machinezoo.fingerprintio.iso19794p2v2005.*;
import com.machinezoo.noexception.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;

class Iso19794p2v2005Codec extends TemplateCodec {
	@Override
	public byte[] encode(List<FeatureTemplate> templates) {
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
	@Override
	public List<FeatureTemplate> decode(byte[] serialized, ExceptionHandler handler) {
		Iso19794p2v2005Template iotemplate = new Iso19794p2v2005Template(serialized, handler);
		TemplateResolution resolution = new TemplateResolution();
		resolution.dpiX = iotemplate.resolutionX * 2.54;
		resolution.dpiY = iotemplate.resolutionY * 2.54;
		return iotemplate.fingerprints.stream()
			.map(fp -> decode(fp, iotemplate, resolution))
			.collect(toList());
	}
	private static Iso19794p2v2005Fingerprint encode(int offset, FeatureTemplate template) {
		Iso19794p2v2005Fingerprint iofingerprint = new Iso19794p2v2005Fingerprint();
		iofingerprint.view = offset;
		iofingerprint.minutiae = template.minutiae.stream()
			.map(m -> encode(m))
			.collect(toList());
		return iofingerprint;
	}
	private static FeatureTemplate decode(Iso19794p2v2005Fingerprint iofingerprint, Iso19794p2v2005Template iotemplate, TemplateResolution resolution) {
		return new FeatureTemplate(
			resolution.decode(iotemplate.width, iotemplate.height),
			iofingerprint.minutiae.stream()
				.map(m -> decode(m, resolution))
				.collect(toList()));
	}
	private static Iso19794p2v2005Minutia encode(FeatureMinutia minutia) {
		Iso19794p2v2005Minutia iominutia = new Iso19794p2v2005Minutia();
		iominutia.positionX = minutia.position.x;
		iominutia.positionY = minutia.position.y;
		iominutia.angle = encodeAngle(minutia.direction);
		iominutia.type = encode(minutia.type);
		return iominutia;
	}
	private static FeatureMinutia decode(Iso19794p2v2005Minutia iominutia, TemplateResolution resolution) {
		return new FeatureMinutia(
			resolution.decode(iominutia.positionX, iominutia.positionY),
			decodeAngle(iominutia.angle),
			decode(iominutia.type));
	}
	private static int encodeAngle(float angle) {
		return (int)Math.round(DoubleAngle.complementary(angle) * DoubleAngle.INV_PI2 * 256) & 0xff;
	}
	private static float decodeAngle(int ioangle) {
		return FloatAngle.complementary(ioangle / 256.0f * FloatAngle.PI2);
	}
	private static Iso19794p2v2005MinutiaType encode(MinutiaType type) {
		switch (type) {
			case ENDING:
				return Iso19794p2v2005MinutiaType.ENDING;
			case BIFURCATION:
				return Iso19794p2v2005MinutiaType.BIFURCATION;
			default :
				return Iso19794p2v2005MinutiaType.ENDING;
		}
	}
	private static MinutiaType decode(Iso19794p2v2005MinutiaType iotype) {
		switch (iotype) {
			case ENDING:
				return MinutiaType.ENDING;
			case BIFURCATION:
				return MinutiaType.BIFURCATION;
			default :
				return MinutiaType.ENDING;
		}
	}
}
