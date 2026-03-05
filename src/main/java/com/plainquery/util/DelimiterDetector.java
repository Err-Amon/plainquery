package com.plainquery.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class DelimiterDetector {

    private static final Logger LOG = Logger.getLogger(DelimiterDetector.class.getName());

    private static final char[] CANDIDATES = {',', '\t', '|', ';'};
    private static final int SAMPLE_LINES = 20;
    private static final char DEFAULT_DELIMITER = ',';

    private DelimiterDetector() {}

    public static char detect(File file, Charset charset) {
        Objects.requireNonNull(file, "File must not be null");
        Objects.requireNonNull(charset, "Charset must not be null");

        Map<Character, int[]> counts = new LinkedHashMap<>();
        for (char c : CANDIDATES) {
            counts.put(c, new int[SAMPLE_LINES]);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset))) {

            int lineIndex = 0;
            String line;
            while ((line = reader.readLine()) != null && lineIndex < SAMPLE_LINES) {
                for (char candidate : CANDIDATES) {
                    counts.get(candidate)[lineIndex] = countOccurrences(line, candidate);
                }
                lineIndex++;
            }

            if (lineIndex == 0) {
                return DEFAULT_DELIMITER;
            }

            char bestDelimiter = DEFAULT_DELIMITER;
            double bestScore = -1.0;

            for (char candidate : CANDIDATES) {
                double score = computeConsistencyScore(counts.get(candidate), lineIndex);
                if (score > bestScore) {
                    bestScore = score;
                    bestDelimiter = candidate;
                }
            }

            return bestDelimiter;

        } catch (IOException e) {
            LOG.warning("Could not detect delimiter for file " + file.getName()
                + ": " + e.getMessage());
            return DEFAULT_DELIMITER;
        }
    }

    private static int countOccurrences(String line, char target) {
        int count = 0;
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == target && !inQuotes) {
                count++;
            }
        }
        return count;
    }

    private static double computeConsistencyScore(int[] counts, int lineCount) {
        if (lineCount == 0) {
            return 0.0;
        }

        int nonZeroLines = 0;
        int sum = 0;
        for (int i = 0; i < lineCount; i++) {
            if (counts[i] > 0) {
                nonZeroLines++;
                sum += counts[i];
            }
        }

        if (nonZeroLines == 0) {
            return 0.0;
        }

        double mean = (double) sum / lineCount;
        if (mean == 0.0) {
            return 0.0;
        }

        double variance = 0.0;
        for (int i = 0; i < lineCount; i++) {
            double diff = counts[i] - mean;
            variance += diff * diff;
        }
        variance /= lineCount;

        double coverageRatio = (double) nonZeroLines / lineCount;
        double consistencyBonus = 1.0 / (1.0 + variance);

        return mean * coverageRatio * consistencyBonus;
    }
}
