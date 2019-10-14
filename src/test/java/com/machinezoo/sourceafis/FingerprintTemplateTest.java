// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import static org.junit.Assert.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import org.junit.*;

public class FingerprintTemplateTest {
	private static FingerprintTemplate t = new FingerprintTemplate();
	public static FingerprintTemplate probe() {
		return new FingerprintTemplate(FingerprintImageTest.probe());
	}
	public static FingerprintTemplate matching() {
		return new FingerprintTemplate(FingerprintImageTest.matching());
	}
	public static FingerprintTemplate nonmatching() {
		return new FingerprintTemplate(FingerprintImageTest.nonmatching());
	}
	@Test public void constructor() {
		probe();
	}
	@Test public void json_roundTrip() {
		TemplateBuilder tb = new TemplateBuilder();
		tb.size = new IntPoint(800, 600);
		tb.minutiae = new ImmutableMinutia[] {
			new ImmutableMinutia(new IntPoint(100, 200), Math.PI, MinutiaType.BIFURCATION),
			new ImmutableMinutia(new IntPoint(300, 400), 0.5 * Math.PI, MinutiaType.ENDING)
		};
		t.immutable = new ImmutableTemplate(tb);
		t = new FingerprintTemplate().deserialize(t.serialize());
		assertEquals(2, t.immutable.minutiae.length);
		ImmutableMinutia a = t.immutable.minutiae[0];
		ImmutableMinutia b = t.immutable.minutiae[1];
		assertEquals(new IntPoint(100, 200), a.position);
		assertEquals(Math.PI, a.direction, 0.0000001);
		assertEquals(MinutiaType.BIFURCATION, a.type);
		assertEquals(new IntPoint(300, 400), b.position);
		assertEquals(0.5 * Math.PI, b.direction, 0.0000001);
		assertEquals(MinutiaType.ENDING, b.type);
	}
	@Test public void randomScaleMatch() throws Exception {
		FingerprintMatcher matcher = new FingerprintMatcher()
			.index(probe());
		DoubleMap original = FingerprintImageTest.matching().decoded;
		int clipX = original.width / 10;
		int clipY = original.height / 10;
		Random random = new Random(0);
		for (int i = 0; i < 10; ++i) {
			DoubleMap clipped = clip(original, random.nextInt(clipY), random.nextInt(clipX), random.nextInt(clipY), random.nextInt(clipX));
			double dpi = 500 + 2 * (random.nextDouble() - 0.5) * 200;
			DoubleMap scaled = TemplateBuilder.scaleImage(clipped, 500 * 1 / (dpi / 500));
			byte[] reencoded = encodeImage(scaled);
			FingerprintTemplate candidate = new FingerprintTemplate(new FingerprintImage()
				.dpi(dpi)
				.decode(reencoded));
			double score = matcher.match(candidate);
			assertTrue("Score " + score + " @ DPI " + dpi, score >= 40);
		}
	}
	private static DoubleMap clip(DoubleMap input, int top, int right, int bottom, int left) {
		DoubleMap output = new DoubleMap(input.width - left - right, input.height - top - bottom);
		for (int y = 0; y < output.height; ++y)
			for (int x = 0; x < output.width; ++x)
				output.set(x, y, input.get(left + x, top + y));
		return output;
	}
	private static byte[] encodeImage(DoubleMap map) throws IOException {
		int[] pixels = new int[map.width * map.height];
		for (int y = 0; y < map.height; ++y)
			for (int x = 0; x < map.width; ++x) {
				int color = (int)Math.round(255 * ((1 - map.get(x, y))));
				pixels[y * map.width + x] = (color << 16) | (color << 8) | color;
			}
		BufferedImage buffered = new BufferedImage(map.width, map.height, BufferedImage.TYPE_INT_RGB);
		buffered.setRGB(0, 0, map.width, map.height, pixels, 0, map.width);
	    ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ImageIO.write(buffered, "BMP", stream);
		return stream.toByteArray();
	}
}
