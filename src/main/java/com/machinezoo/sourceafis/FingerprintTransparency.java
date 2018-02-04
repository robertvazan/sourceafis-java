// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.io.*;
import java.util.*;
import gnu.trove.map.hash.*;

public abstract class FingerprintTransparency {
	static final FingerprintTransparency none = new FingerprintTransparency() {
		@Override protected void log(String path, InputStream json, InputStream binary) {
		}
	};
	protected abstract void log(String path, InputStream json, InputStream binary);
	public void add(String name, byte[] image, double dpi) {
		logImageOriginal(image, dpi);
	}
	public void deserialize(String name, String template) {
	}
	public void convert(String name, byte[] iso) {
		logIsoTemplate(iso);
	}
	public void attach(String name, byte[] image, double dpi) {
	}
	public void match(String probe, String candidate) {
	}
	boolean logging() {
		return this != none;
	}
	private void logImageOriginal(byte[] image, double dpi) {
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
	void logMinutiaeSkeleton(FingerprintMinutia[] minutiae) {
	}
	void logMinutiaeInner(FingerprintMinutia[] minutiae) {
	}
	void logMinutiaeRemovedClouds(FingerprintMinutia[] minutiae) {
	}
	void logMinutiaeClipped(FingerprintMinutia[] minutiae) {
	}
	void logMinutiaeShuffled(FingerprintMinutia[] minutiae) {
	}
	void logEdgeTable(NeighborEdge[][] table) {
	}
	void logMinutiaeDeserialized(FingerprintMinutia[] minutiae) {
	}
	private void logIsoTemplate(byte[] iso) {
	}
	void logIsoDimensions(int width, int height, int cmPixelsX, int cmPixelsY) {
	}
	void logMinutiaeIso(FingerprintMinutia[] minutiae) {
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
