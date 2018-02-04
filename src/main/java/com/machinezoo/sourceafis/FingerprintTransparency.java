// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import gnu.trove.map.hash.*;
import lombok.*;

public abstract class FingerprintTransparency implements AutoCloseable {
	private static final ThreadLocal<FingerprintTransparency> current = new ThreadLocal<>();
	private FingerprintTransparency outer;
	private boolean closed;
	static final FingerprintTransparency none = new FingerprintTransparency() {
		@Override protected void log(String path, Map<String, InputStream> data) {
		}
	};
	protected abstract void log(String path, Map<String, InputStream> data);
	protected FingerprintTransparency() {
		outer = current.get();
		current.set(this);
	}
	@Override public void close() {
		if (!closed) {
			closed = true;
			current.set(outer);
			outer = null;
		}
	}
	static FingerprintTransparency current() {
		return Optional.ofNullable(current.get()).orElse(none);
	}
	public static FingerprintTransparency zip(ZipOutputStream zip) {
		byte[] buffer = new byte[4096];
		return new FingerprintTransparency() {
			int offset;
			@Override @SneakyThrows protected void log(String path, Map<String, InputStream> data) {
				++offset;
				for (String suffix : data.keySet()) {
					zip.putNextEntry(new ZipEntry(String.format("%02d", offset) + "-" + path + suffix));
					InputStream stream = data.get(suffix);
					while (true) {
						int read = stream.read(buffer);
						if (read <= 0)
							break;
						zip.write(buffer, 0, read);
					}
					zip.closeEntry();
				}
			}
			@Override @SneakyThrows public void close() {
				super.close();
				zip.close();
			}
		};
	}
	boolean logging() {
		return this != none;
	}
	void logImageDecoded(DoubleMap image) {
	}
	void logImageScaled(DoubleMap image) {
	}
	void logBlockMap(BlockMap blocks) {
	}
	void logBlockHistogram(Histogram histogram) {
	}
	void logSmoothedHistogram(Histogram histogram) {
	}
	void logContrastClipped(DoubleMap contrast) {
	}
	void logContrastAbsolute(BooleanMap mask) {
	}
	void logContrastRelative(BooleanMap mask) {
	}
	void logContrastCombined(BooleanMap mask) {
	}
	void logContrastFiltered(BooleanMap mask) {
	}
	void logEqualized(DoubleMap image) {
	}
	void logOrientationPixelwise(PointMap orientations) {
	}
	void logOrientationBlocks(PointMap orientations) {
	}
	void logOrientationSmoothed(PointMap orientations) {
	}
	void logParallelSmoothing(DoubleMap smoothed) {
	}
	void logOrthogonalSmoothing(DoubleMap smoothed) {
	}
	void logBinarized(BooleanMap image) {
	}
	void logBinarizedFiltered(BooleanMap image) {
	}
	void logPixelMask(BooleanMap image) {
	}
	void logInnerMask(BooleanMap image) {
	}
	void logSkeletonBinarized(SkeletonType type, BooleanMap image) {
	}
	void logThinned(SkeletonType type, BooleanMap image) {
	}
	void logTraced(SkeletonType type, List<SkeletonMinutia> minutiae) {
	}
	void logRemovedDots(SkeletonType type, List<SkeletonMinutia> minutiae) {
	}
	void logRemovedPores(SkeletonType type, List<SkeletonMinutia> minutiae) {
	}
	void logRemovedGaps(SkeletonType type, List<SkeletonMinutia> minutiae) {
	}
	void logRemovedTails(SkeletonType type, List<SkeletonMinutia> minutiae) {
	}
	void logRemovedFragments(SkeletonType type, List<SkeletonMinutia> minutiae) {
	}
	void logMinutiaeSkeleton(Minutia[] minutiae) {
	}
	void logMinutiaeInner(Minutia[] minutiae) {
	}
	void logMinutiaeRemovedClouds(Minutia[] minutiae) {
	}
	void logMinutiaeClipped(Minutia[] minutiae) {
	}
	void logMinutiaeShuffled(Minutia[] minutiae) {
	}
	void logEdgeTable(NeighborEdge[][] table) {
	}
	void logMinutiaeDeserialized(Minutia[] minutiae) {
	}
	void logIsoDimensions(int width, int height, int cmPixelsX, int cmPixelsY) {
	}
	void logMinutiaeIso(Minutia[] minutiae) {
	}
	void logEdgeHash(TIntObjectHashMap<List<IndexedEdge>> edgeHash) {
	}
	void logRoot(MinutiaPair root) {
	}
	void logSupportingEdge(MinutiaPair pair) {
	}
	void logPairing(int count, MinutiaPair[] pairs) {
	}
	void logScore(double minutia, double ratio, double supported, double edge, double type, double distance, double angle, double total) {
	}
	void logShapedScore(double shaped) {
	}
}
