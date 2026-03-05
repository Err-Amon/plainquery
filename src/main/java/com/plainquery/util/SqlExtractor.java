package com.plainquery.util;

import com.plainquery.exception.SqlExtractionException;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlExtractor {

    private static final Pattern CODE_BLOCK = Pattern.compile(
        "```(?:sql)?\\s*\\n?(.*?)\\n?```",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern SELECT_STATEMENT = Pattern.compile(
        "(SELECT\\b[^;]*)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private SqlExtractor() {}

    public static String extract(String aiResponse) throws SqlExtractionException {
        if (aiResponse == null) {
            throw new SqlExtractionException("AI response must not be null");
        }

        String trimmed = aiResponse.trim();
        if (trimmed.isEmpty()) {
            throw new SqlExtractionException("AI response is empty");
        }

        String fromCodeBlock = extractFromCodeBlock(trimmed);
        if (fromCodeBlock != null) {
            return fromCodeBlock.trim();
        }

        String fromSelect = extractFromSelectKeyword(trimmed);
        if (fromSelect != null) {
            return fromSelect.trim();
        }

        String upperTrimmed = trimmed.toUpperCase();
        if (upperTrimmed.startsWith("SELECT")) {
            String cleaned = trimmed.trim();
            if (!cleaned.endsWith(";")) {
                cleaned = cleaned + ";";
            }
            if (!cleaned.isEmpty()) {
                return cleaned;
            }
        }

        throw new SqlExtractionException(
            "No SQL SELECT statement found in AI response. Response began with: "
            + trimmed.substring(0, Math.min(trimmed.length(), 80)));
    }

    private static String extractFromCodeBlock(String response) {
        Matcher matcher = CODE_BLOCK.matcher(response);
        String last = null;
        while (matcher.find()) {
            String candidate = matcher.group(1).trim();
            if (candidate.toUpperCase().contains("SELECT")) {
                last = candidate.trim();
                if (!last.endsWith(";")) {
                    last = last + ";";
                }
            }
        }
        return last;
    }

    private static String extractFromSelectKeyword(String response) {
        Matcher matcher = SELECT_STATEMENT.matcher(response);
        String last = null;
        while (matcher.find()) {
            String candidate = matcher.group(1).trim();
            last = candidate.trim();
            if (!last.endsWith(";")) {
                last = last + ";";
            }
        }
        return last;
    }
}
