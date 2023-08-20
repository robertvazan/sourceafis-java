// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.primitives;

/*
 * JOL library can measure memory footprint of objects,
 * but its recent versions require special permissions (granted on command line or otherwise)
 * and it is not a Java module. It now prints ugly warnings to the console.
 * JOL development is essentially halted.
 * So we use our own somewhat crude estimate instead.
 */
public class MemoryEstimates {
    private static int detectBitness() {
        /*
         * Source: https://www.baeldung.com/java-detect-jvm-64-or-32-bit
         * This will work on Oracle Java only.
         */
        var model = System.getProperty("sun.arch.data.model", "unknown");
        if (model.equals("32"))
            return 32;
        if (model.equals("64"))
            return 64;
        var arch = System.getProperty("os.arch", "unknown");
        switch (arch) {
            /*
             * Source of constants: https://stackoverflow.com/a/2062045
             */
            case "x86":
                return 32;
            case "amd64":
                return 64;
            /*
             * Source of constants: https://github.com/tnakamot/java-os-detector/blob/master/src/main/java/com/github/tnakamot/os/Detector.java
             * Patterns that match '32' or '64' are handled in general way below.
             */
            case "ia32e":
                return 64;
            case "i386":
            case "i486":
            case "i586":
            case "i686":
            case "ia64n":
                return 32;
            case "sparc":
            case "arm":
            case "mips":
            case "mipsel":
            case "ppc":
            case "ppcle":
            case "s390":
                return 32;
            default:
                if (arch.contains("64"))
                    return 64;
                if (arch.contains("32"))
                    return 32;
                /*
                 * Assume 64-bit JVM unless we have proof to the contrary.
                 */
                return 64;
        }
    }
    private static final int BITNESS = detectBitness();
    /*
     * Assume compressed 32-bit references even on 64-bit platforms.
     * This will be wrong on 32GB+ heaps or when compressed references are disabled (-XX:-UseCompressedOops).
     */
    public static final int REFERENCE = 4;
    /*
     * Mark word in standard object layout matches platform bitness.
     */
    private static final int MARK = BITNESS / 8;
    /*
     * Assume standard object layout: mark word + class pointer.
     */
    private static final int OBJECT_HEADER = MARK + REFERENCE;
    /*
     * Assume that padding ensures alignment of longs and doubles even on 32-bit platforms.
     */
    private static final int PADDING = 8;
    private static int pad(int padding, int size) { return (size + padding - 1) / padding * padding; }
    private static int pad(int size) { return pad(PADDING, size); }
    /*
     * Assume optimal field layout in memory.
     * Alignment refers to minimum alignment of the field block, which is usually equal to the largest field.
     */
    public static int object(int fields, int alignment) { return pad(pad(alignment, OBJECT_HEADER) + fields); }
    /*
     * Assume standard array layout: object header + 32-bit length.
     */
    private static final int ARRAY_HEADER = OBJECT_HEADER + 4;
    public static int array(int component, int count) {
        /*
         * Pad array header to ensure alignment of all array items.
         */
        return pad(pad(component, ARRAY_HEADER) + component * count);
    }
}
