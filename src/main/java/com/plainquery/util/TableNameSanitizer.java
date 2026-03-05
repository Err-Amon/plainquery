package com.plainquery.util;

import java.util.Objects;

public final class TableNameSanitizer {

    private static final int MAX_LENGTH = 60;

    private TableNameSanitizer() {}

    public static String sanitize(String filename) {
        Objects.requireNonNull(filename, "Filename must not be null");
        if (filename.isBlank()) {
            throw new IllegalArgumentException("Filename must not be blank");
        }

        String base = stripExtension(filename.trim());
        String replaced = base.replaceAll("[^a-zA-Z0-9_]", "_");

        if (replaced.isEmpty()) {
            replaced = "table";
        }

        if (Character.isDigit(replaced.charAt(0))) {
            replaced = "t_" + replaced;
        }

        if (replaced.length() > MAX_LENGTH) {
            replaced = replaced.substring(0, MAX_LENGTH);
        }

        return replaced.toLowerCase();
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            return filename.substring(0, dot);
        }
        return filename;
    }
}
