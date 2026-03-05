package com.plainquery.util;

import com.plainquery.exception.InsufficientMemoryException;

import java.util.logging.Logger;

public final class MemoryGuard {

    private static final Logger LOG = Logger.getLogger(MemoryGuard.class.getName());

    private static final double SAFETY_MARGIN = 0.15;

    private MemoryGuard() {}

    public static void assertSufficientMemory(long estimatedBytes)
            throws InsufficientMemoryException {

        if (estimatedBytes < 0) {
            throw new IllegalArgumentException("Estimated bytes must not be negative");
        }

        Runtime runtime = Runtime.getRuntime();
        long maxMemory   = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory  = runtime.freeMemory();

        long usableMemory = maxMemory - totalMemory + freeMemory;
        long safeThreshold = (long) (maxMemory * SAFETY_MARGIN);
        long availableForOperation = usableMemory - safeThreshold;

        LOG.fine(String.format(
            "MemoryGuard: estimated=%d bytes, usable=%d bytes, threshold=%d bytes",
            estimatedBytes, usableMemory, safeThreshold));

        if (estimatedBytes > availableForOperation) {
            throw new InsufficientMemoryException(String.format(
                "Insufficient heap memory for operation. "
                + "Required: %d MB, Available (after safety margin): %d MB. "
                + "Consider switching to file-based database mode or reducing file size.",
                toMb(estimatedBytes),
                toMb(Math.max(availableForOperation, 0))));
        }
    }

    public static long availableBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
    }

    private static long toMb(long bytes) {
        return bytes / (1024L * 1024L);
    }
}
