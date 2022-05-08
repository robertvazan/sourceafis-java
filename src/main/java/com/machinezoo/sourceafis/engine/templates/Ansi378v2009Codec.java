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
	public byte[] encode(List<MutableTemplate> templates) {
		Ansi378v2009Template iotemplate = new Ansi378v2009Template();
		iotemplate.fingerprints = IntStream.range(0, templates.size())
			.mapToObj(n -> encode(n, templates.get(n)))
			.collect(toList());
		return iotemplate.toByteArray();
	}
	@Override
	public List<MutableTemplate> decode(byte[] serialized, ExceptionHandler handler) {
		return new Ansi378v2009Template(serialized, handler).fingerprints.stream()
			.map(fp -> decode(fp))
			.collect(toList());
	}
	private static Ansi378v2009Fingerprint encode(int offset, MutableTemplate template) {
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
	private static MutableTemplate decode(Ansi378v2009Fingerprint iofingerprint) {
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
	private static Ansi378v2009Minutia encode(MutableMinutia minutia) {
		Ansi378v2009Minutia iominutia = new Ansi378v2009Minutia();
		iominutia.positionX = minutia.position.x;
		iominutia.positionY = minutia.position.y;
		iominutia.angle = encodeAngle(minutia.direction);
		iominutia.type = encode(minutia.type);
		return iominutia;
	}
	private static MutableMinutia decode(Ansi378v2009Minutia iominutia, TemplateResolution resolution) {
		MutableMinutia minutia = new MutableMinutia();
		minutia.position = resolution.decode(iominutia.positionX, iominutia.positionY);
		minutia.direction = decodeAngle(iominutia.angle);
		minutia.type = decode(iominutia.type);
		return minutia;
	}
	private static int encodeAngle(double angle) {
		return (int)Math.ceil(DoubleAngle.complementary(angle) * DoubleAngle.INV_PI2 * 360 / 2) % 180;
	}
	private static double decodeAngle(int ioangle) {
		return DoubleAngle.complementary(((2 * ioangle - 1 + 360) % 360) / 360.0 * DoubleAngle.PI2);
	}
	private static Ansi378v2009MinutiaType encode(MinutiaType type) {
		switch (type) {
			case ENDING :
				return Ansi378v2009MinutiaType.ENDING;
			case BIFURCATION :
				return Ansi378v2009MinutiaType.BIFURCATION;
			default :
				return Ansi378v2009MinutiaType.ENDING;
		}
	}
	private static MinutiaType decode(Ansi378v2009MinutiaType iotype) {
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
