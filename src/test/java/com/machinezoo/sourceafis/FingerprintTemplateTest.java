// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import org.junit.jupiter.api.*;
import com.machinezoo.sourceafis.engine.extractor.*;
import com.machinezoo.sourceafis.engine.features.*;
import com.machinezoo.sourceafis.engine.primitives.*;
import com.machinezoo.sourceafis.engine.templates.*;

public class FingerprintTemplateTest {
	public static FingerprintTemplate probe() {
		return new FingerprintTemplate(FingerprintImageTest.probe());
	}
	public static FingerprintTemplate matching() {
		return new FingerprintTemplate(FingerprintImageTest.matching());
	}
	public static FingerprintTemplate nonmatching() {
		return new FingerprintTemplate(FingerprintImageTest.nonmatching());
	}
	public static FingerprintTemplate probeGray() {
		return new FingerprintTemplate(FingerprintImageTest.probeGray());
	}
	public static FingerprintTemplate matchingGray() {
		return new FingerprintTemplate(FingerprintImageTest.matchingGray());
	}
	public static FingerprintTemplate nonmatchingGray() {
		return new FingerprintTemplate(FingerprintImageTest.nonmatchingGray());
	}
	@Test
	public void constructor() {
		probe();
	}
	@Test
	public void roundTripSerialization() {
		var mt = new FeatureTemplate(
			new IntPoint(800, 600),
			List.of(
				new FeatureMinutia(new IntPoint(100, 200), FloatAngle.PI, MinutiaType.BIFURCATION),
				new FeatureMinutia(new IntPoint(300, 400), FloatAngle.HALF_PI, MinutiaType.ENDING)));
		FingerprintTemplate t = new FingerprintTemplate(new SearchTemplate(mt));
		t = new FingerprintTemplate(t.toByteArray());
		assertEquals(2, t.inner.minutiae.length);
		var a = t.inner.minutiae[0];
		var b = t.inner.minutiae[1];
		assertEquals(100, a.x);
		assertEquals(200, a.y);
		assertEquals(Math.PI, a.direction, 0.0000001);
		assertEquals(MinutiaType.BIFURCATION, a.type);
		assertEquals(300, b.x);
		assertEquals(400, b.y);
		assertEquals(0.5 * Math.PI, b.direction, 0.0000001);
		assertEquals(MinutiaType.ENDING, b.type);
	}
	@Test
	public void randomScaleMatch() throws Exception {
		FingerprintMatcher matcher = new FingerprintMatcher(probe());
		DoubleMatrix original = FingerprintImageTest.matching().matrix;
		int clipX = original.width / 10;
		int clipY = original.height / 10;
		Random random = new Random(0);
		for (int i = 0; i < 10; ++i) {
			DoubleMatrix clipped = clip(original, random.nextInt(clipY), random.nextInt(clipX), random.nextInt(clipY), random.nextInt(clipX));
			double dpi = 500 + 2 * (random.nextDouble() - 0.5) * 200;
			DoubleMatrix scaled = ImageResizer.resize(clipped, 500 * 1 / (dpi / 500));
			byte[] reencoded = encodeImage(scaled);
			FingerprintTemplate candidate = new FingerprintTemplate(
				new FingerprintImage(reencoded, new FingerprintImageOptions()
					.dpi(dpi)));
			double score = matcher.match(candidate);
			assertTrue(score >= 40, "Score " + score + " @ DPI " + dpi);
		}
	}
	private static DoubleMatrix clip(DoubleMatrix input, int top, int right, int bottom, int left) {
		DoubleMatrix output = new DoubleMatrix(input.width - left - right, input.height - top - bottom);
		for (int y = 0; y < output.height; ++y)
			for (int x = 0; x < output.width; ++x)
				output.set(x, y, input.get(left + x, top + y));
		return output;
	}
	private static byte[] encodeImage(DoubleMatrix matrix) throws IOException {
		int[] pixels = new int[matrix.width * matrix.height];
		for (int y = 0; y < matrix.height; ++y)
			for (int x = 0; x < matrix.width; ++x) {
				int color = (int)Math.round(255 * ((1 - matrix.get(x, y))));
				pixels[y * matrix.width + x] = (color << 16) | (color << 8) | color;
			}
		BufferedImage buffered = new BufferedImage(matrix.width, matrix.height, BufferedImage.TYPE_INT_RGB);
		buffered.setRGB(0, 0, matrix.width, matrix.height, pixels, 0, matrix.width);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ImageIO.write(buffered, "BMP", stream);
		return stream.toByteArray();
	}
}
