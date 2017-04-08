package sourceafis;

import sourceafis.collections.*;
import sourceafis.scalars.*;

public class FingerprintTemplate {
	public FingerprintTemplate(DoubleMap image) {
		this(image, 500);
	}
	public FingerprintTemplate(DoubleMap image, double dpi) {
		final int blockSize = 15;
		final double dpiTolerance = 10;
		if (Math.abs(dpi - 500) > dpiTolerance)
			image = scaleImage(image, dpi);
		BlockMap blocks = new BlockMap(image.width, image.height, blockSize);
		Histogram histogram = computeHistogram(blocks, image);
		Histogram smoothHistogram = smoothHistogram(blocks, histogram);
	}
	static DoubleMap scaleImage(DoubleMap input, double dpi) {
		return scaleImage(input, (int)Math.round(500.0 / dpi * input.width), (int)Math.round(500.0 / dpi * input.height));
	}
	static DoubleMap scaleImage(DoubleMap input, int newWidth, int newHeight) {
		DoubleMap output = new DoubleMap(newWidth, newHeight);
		double scaleX = newWidth / (double)input.width;
		double scaleY = newHeight / (double)input.height;
		double descaleX = 1 / scaleX;
		double descaleY = 1 / scaleY;
		for (int y = 0; y < newHeight; ++y) {
			double y1 = y * descaleY;
			double y2 = y1 + descaleY;
			int y1i = (int)y1;
			int y2i = (int)Math.ceil(y2);
			for (int x = 0; x < newWidth; ++x) {
				double x1 = x * descaleX;
				double x2 = x1 + descaleX;
				int x1i = (int)x1;
				int x2i = (int)Math.ceil(x2);
				double sum = 0;
				for (int oy = y1i; oy < y2i; ++oy) {
					double ry = Math.min(oy + 1, y2) - Math.max(oy, y1);
					for (int ox = x1i; ox < x2i; ++ox) {
						double rx = Math.min(ox + 1, x2) - Math.max(ox, x1);
						sum += rx * ry * input.get(ox, oy);
					}
				}
				output.set(x, y, sum * (scaleX * scaleY));
			}
		}
		return output;
	}
	static Histogram computeHistogram(BlockMap blocks, DoubleMap image) {
		final int histogramDepth = 100;
		Histogram histogram = new Histogram(blocks.blockCount.y, blocks.blockCount.x, histogramDepth);
		for (Cell block : blocks.blockCount) {
			Block area = blocks.blockAreas.get(block);
			for (int y = area.bottom(); y < area.top(); ++y)
				for (int x = area.left(); x < area.right(); ++x) {
					int depth = (int)(image.get(x, y) * histogram.depth);
					histogram.increment(block.x, block.y, Math.max(0, Math.min(histogram.depth - 1, depth)));
				}
		}
		return histogram;
	}
	static Histogram smoothHistogram(BlockMap blocks, Histogram input) {
		Cell[] blocksAround = new Cell[] { new Cell(0, 0), new Cell(-1, 0), new Cell(0, -1), new Cell(-1, -1) };
		Histogram output = new Histogram(blocks.cornerCount.y, blocks.cornerCount.x, input.depth);
		for (Cell corner : blocks.cornerCount) {
			for (Cell relative : blocksAround) {
				Cell block = corner.plus(relative);
				if (blocks.blockCount.contains(block)) {
					for (int i = 0; i < input.depth; ++i)
						output.add(corner.x, corner.y, i, input.get(block.x, block.y, i));
				}
			}
		}
		return output;
	}
}
