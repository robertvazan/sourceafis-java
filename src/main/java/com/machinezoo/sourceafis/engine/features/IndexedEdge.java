// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.features;

import com.machinezoo.sourceafis.engine.primitives.*;

public class IndexedEdge extends EdgeShape {
    private final byte reference;
    private final byte neighbor;
    public IndexedEdge(SearchMinutia[] minutiae, int reference, int neighbor) {
        super(minutiae[reference], minutiae[neighbor]);
        this.reference = (byte)reference;
        this.neighbor = (byte)neighbor;
    }
    public int reference() { return Byte.toUnsignedInt(reference); }
    public int neighbor() { return Byte.toUnsignedInt(neighbor); }
    public static int memory() { return MemoryEstimates.object(Short.BYTES + 2 * Float.BYTES + 2 * Byte.BYTES, Float.BYTES); }
}
