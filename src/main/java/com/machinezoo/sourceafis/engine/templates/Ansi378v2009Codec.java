// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.templates;

import static java.util.stream.Collectors.*;
import java.util.*;
import java.util.stream.*;
import com.machinezoo.fingerprintio.ansi378v2009.*;
import com.machinezoo.noexception.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;

class Ansi378v2009Codec extends TemplateCodec {
	@Override
	public byte[] encode(List<FeatureTemplate> templates) {
		Ansi378v2009Template iotemplate = new Ansi378v2009Template();
		iotemplate.fingerprints = IntStream.range(0, templates.size())
			.mapToObj(n -> encode(n, templates.get(n)))
			.collect(toList());
		return iotemplate.toByteArray();
	}
	@Override
	public List<FeatureTemplate> decode(byte[] serialized, ExceptionHandler handler) {
		return new Ansi378v2009Template(serialized, handler).fingerprints.stream()
			.map(fp -> decode(fp))
			.collect(toList());
	}
	private static Ansi378v2009Fingerprint encode(int offset, FeatureTemplate template) {
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
	private static FeatureTemplate decode(Ansi378v2009Fingerprint iofingerprint) {
		TemplateResolution resolution = new TemplateResolution();
		resolution.dpiX = iofingerprint.resolutionX * 2.54;
		resolution.dpiY = iofingerprint.resolutionY * 2.54;
		return new FeatureTemplate(
			resolution.decode(iofingerprint.width, iofingerprint.height),
			iofingerprint.minutiae.stream()
				.map(m -> decode(m, resolution))
				.collect(toList()));
	}
	private static Ansi378v2009Minutia encode(FeatureMinutia minutia) {
		Ansi378v2009Minutia iominutia = new Ansi378v2009Minutia();
		iominutia.positionX = minutia.position.x;
		iominutia.positionY = minutia.position.y;
		iominutia.angle = encodeAngle(minutia.direction);
		iominutia.type = encode(minutia.type);
		return iominutia;
	}
	private static FeatureMinutia decode(Ansi378v2009Minutia iominutia, TemplateResolution resolution) {
		return new FeatureMinutia(
			resolution.decode(iominutia.positionX, iominutia.positionY),
			decodeAngle(iominutia.angle),
			decode(iominutia.type));
	}
	private static int encodeAngle(float angle) {
		return (int)Math.ceil(DoubleAngle.complementary(angle) * DoubleAngle.INV_PI2 * 360 / 2) % 180;
	}
	private static float decodeAngle(int ioangle) {
		return FloatAngle.complementary(((2 * ioangle - 1 + 360) % 360) / 360.0f * FloatAngle.PI2);
	}
	private static Ansi378v2009MinutiaType encode(MinutiaType type) {
		switch (type) {
			case ENDING:
				return Ansi378v2009MinutiaType.ENDING;
			case BIFURCATION:
				return Ansi378v2009MinutiaType.BIFURCATION;
			default :
				return Ansi378v2009MinutiaType.ENDING;
		}
	}
	private static MinutiaType decode(Ansi378v2009MinutiaType iotype) {
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
