package com.matchbar.util;

import java.nio.charset.StandardCharsets;

/**
 * Detecta el tipo real de un fichero por sus primeros bytes ("magic numbers").
 * El Content-Type que envía el cliente lo elige él y no es fiable.
 */
public final class FileSignatures {

    private FileSignatures() {}

    public static boolean isJpeg(byte[] b) {
        return startsWith(b, 0, 0xFF, 0xD8, 0xFF);
    }

    public static boolean isPng(byte[] b) {
        return startsWith(b, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
    }

    public static boolean isWebp(byte[] b) {
        return startsWith(b, 0, 'R', 'I', 'F', 'F') && startsWith(b, 8, 'W', 'E', 'B', 'P');
    }

    public static boolean isPdf(byte[] b) {
        return b != null && b.length >= 5
                && new String(b, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
    }

    private static boolean startsWith(byte[] b, int offset, int... expected) {
        if (b == null || b.length < offset + expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if ((b[offset + i] & 0xFF) != expected[i]) return false;
        }
        return true;
    }
}
