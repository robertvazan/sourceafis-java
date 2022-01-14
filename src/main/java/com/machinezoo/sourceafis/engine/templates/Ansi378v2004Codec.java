// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.templates;

import static java.util.stream.Collectors.*;
import java.util.*;
import java.util.stream.*;
import com.machinezoo.fingerprintio.ansi378v2004.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;

class Ansi378v2004Codec extends TemplateCodec {
	@Override
	public byte[] encode(List<MutableTemplate> templates) {
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
	@Override
	public List<MutableTemplate> decode(byte[] serialized, boolean strict) {
		Ansi378v2004Template iotemplate = new Ansi378v2004Template(serialized, strict);
		TemplateResolution resolution = new TemplateResolution();
		resolution.dpiX = iotemplate.resolutionX * 2.54;
		resolution.dpiY = iotemplate.resolutionY * 2.54;
		return iotemplate.fingerprints.stream()
			.map(fp -> decode(fp, iotemplate, resolution))
			.collect(toList());
	}
	private static Ansi378v2004Fingerprint encode(int offset, MutableTemplate template) {
		Ansi378v2004Fingerprint iofingerprint = new Ansi378v2004Fingerprint();
		iofingerprint.view = offset;
		iofingerprint.minutiae = template.minutiae.stream()
			.map(m -> encode(m))
			.collect(toList());
		return iofingerprint;
	}
	private static MutableTemplate decode(Ansi378v2004Fingerprint iofingerprint, Ansi378v2004Template iotemplate, TemplateResolution resolution) {
		MutableTemplate template = new MutableTemplate();
		template.size = resolution.decode(iotemplate.width, iotemplate.height);
		template.minutiae = iofingerprint.minutiae.stream()
			.map(m -> decode(m, resolution))
			.collect(toList());
		return template;
	}
	private static Ansi378v2004Minutia encode(MutableMinutia minutia) {
		Ansi378v2004Minutia iominutia = new Ansi378v2004Minutia();
		iominutia.positionX = minutia.position.x;
		iominutia.positionY = minutia.position.y;
		iominutia.angle = encodeAngle(minutia.direction);
		iominutia.type = encode(minutia.type);
		return iominutia;
	}
	private static MutableMinutia decode(Ansi378v2004Minutia iominutia, TemplateResolution resolution) {
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
	private static Ansi378v2004MinutiaType encode(MinutiaType type) {
		switch (type) {
			case ENDING :
				return Ansi378v2004MinutiaType.ENDING;
			case BIFURCATION :
				return Ansi378v2004MinutiaType.BIFURCATION;
			default :
				return Ansi378v2004MinutiaType.ENDING;
		}
	}
	private static MinutiaType decode(Ansi378v2004MinutiaType iotype) {
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
