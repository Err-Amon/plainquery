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
        "(SELECT\\b[\\s\\S]*?\\bFROM\\b[^;\\n]*)",
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
            String sanitized = sanitizeSql(fromCodeBlock);
            return sanitized;
        }

        String fromSelect = extractFromSelectKeyword(trimmed);
        if (fromSelect != null) {
            String sanitized = sanitizeSql(fromSelect);
            return sanitized;
        }

        String upperTrimmed = trimmed.toUpperCase();
        if (upperTrimmed.contains("SELECT")) {
            // Find the SELECT statement in the response
            int selectStart = upperTrimmed.indexOf("SELECT");
            int selectEnd = findSelectEnd(trimmed, selectStart);
            
            String extractedSql = trimmed.substring(selectStart, selectEnd + 1).trim();
            
            // Remove semicolon if present at end
            if (extractedSql.endsWith(";")) {
                extractedSql = extractedSql.substring(0, extractedSql.length() - 1).trim();
            }
            
            if (!extractedSql.isEmpty()) {
                String sanitized = sanitizeSql(extractedSql);
                return sanitized;
            }
        }

        throw new SqlExtractionException(
            "No SQL SELECT statement found in AI response. Response began with: "
            + trimmed.substring(0, Math.min(trimmed.length(), 80)));
    }
    
    private static int findSelectEnd(String response, int startIndex) {
        int endIndex = response.length() - 1;
        
        // Look for typical end indicators
        int semicolonIndex = response.indexOf(";", startIndex);
        int newLineIndex = response.indexOf("\n", startIndex);
        
        if (semicolonIndex != -1 && semicolonIndex < endIndex) {
            endIndex = semicolonIndex;
        }
        
        if (newLineIndex != -1 && newLineIndex < endIndex) {
            endIndex = newLineIndex - 1; // exclude the newline character
        }
        
        return endIndex;
    }

    private static String extractFromCodeBlock(String response) {
        Matcher matcher = CODE_BLOCK.matcher(response);
        String last = null;
        while (matcher.find()) {
            String candidate = matcher.group(1).trim();
            if (isValidSelect(candidate)) {
                last = candidate.trim();
                if (last.endsWith(";")) {
                    last = last.substring(0, last.length() - 1).trim();
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
            if (isValidSelect(candidate)) {
                last = candidate.trim();
                if (last.endsWith(";")) {
                    last = last.substring(0, last.length() - 1).trim();
                }
            }
        }
        return last;
    }

    private static boolean isValidSelect(String candidate) {
        if (candidate == null) return false;
        String up = candidate.toUpperCase();
        // Must contain FROM and SELECT
        return up.contains("SELECT") && up.contains(" FROM ");
    }

    private static String sanitizeSql(String sql) {
        if (sql == null) return null;
        String s = sql.trim();
        // Fix quoted star: SELECT "*" -> SELECT *
        s = s.replaceAll("SELECT\\s+\"\\*\"", "SELECT *");
        s = s.replaceAll("select\\s+\"\\*\"", "select *");
        // Collapse multiple spaces
        s = s.replaceAll("\\s+", " ");
        return s.trim();
    }
}
