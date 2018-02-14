// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import java.io.*;
import java.nio.*;
import java.util.*;
import gnu.trove.map.hash.*;
import lombok.*;

class IndexedEdge extends EdgeShape {
	final int reference;
	final int neighbor;
	IndexedEdge(Minutia[] minutiae, int reference, int neighbor) {
		super(minutiae[reference], minutiae[neighbor]);
		this.reference = reference;
		this.neighbor = neighbor;
	}
	@SneakyThrows void write(DataOutputStream stream) {
		stream.writeInt(reference);
		stream.writeInt(neighbor);
		stream.writeInt(length);
		stream.writeDouble(referenceAngle);
		stream.writeDouble(neighborAngle);
	}
	static LazyByteStream stream(TIntObjectHashMap<List<IndexedEdge>> hash) {
		return new LazyByteStream(() -> serialize(hash));
	}
	@SneakyThrows static ByteBuffer serialize(TIntObjectHashMap<List<IndexedEdge>> hash) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream formatter = new DataOutputStream(buffer);
		int[] keys = hash.keys();
		Arrays.sort(keys);
		formatter.writeInt(keys.length);
		for (int key : keys) {
			formatter.writeInt(key);
			List<IndexedEdge> edges = hash.get(key);
			formatter.writeInt(edges.size());
			for (IndexedEdge edge : edges)
				edge.write(formatter);
		}
		formatter.close();
		return ByteBuffer.wrap(buffer.toByteArray());
	}
}
