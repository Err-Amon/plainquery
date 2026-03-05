package com.plainquery.util;

import com.plainquery.exception.SqlValidationException;

import java.util.Objects;
import java.util.regex.Pattern;

public final class SqlValidator {

    private static final Pattern LEADING_SELECT = Pattern.compile(
        "^\\s*SELECT\\b.*",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final String[] FORBIDDEN_KEYWORDS = {
        ";",
        "PRAGMA",
        "ATTACH",
        "DETACH",
        "DROP",
        "DELETE",
        "UPDATE",
        "INSERT",
        "CREATE",
        "ALTER",
        "REPLACE",
        "TRUNCATE",
        "VACUUM",
        "REINDEX",
        "ANALYZE"
    };

    private SqlValidator() {}

    public static void validate(String sql) throws SqlValidationException {
        if (sql == null) {
            throw new SqlValidationException("SQL must not be null");
        }

        String trimmed = sql.trim();

        if (trimmed.isEmpty()) {
            throw new SqlValidationException("SQL statement must not be empty");
        }

        if (!LEADING_SELECT.matcher(trimmed).matches()) {
            throw new SqlValidationException(
                "Only SELECT statements are permitted. Received: "
                + trimmed.substring(0, Math.min(trimmed.length(), 40)));
        }

        String upper = trimmed.toUpperCase();

        for (String forbidden : FORBIDDEN_KEYWORDS) {
            if (forbidden.equals(";")) {
                if (trimmed.contains(";")) {
                    throw new SqlValidationException(
                        "SQL must not contain semicolons. Multiple statements are not permitted.");
                }
                continue;
            }

            int idx = upper.indexOf(forbidden);
            while (idx >= 0) {
                if (isBoundary(upper, idx, forbidden.length())) {
                    throw new SqlValidationException(
                        "SQL contains forbidden keyword: " + forbidden);
                }
                idx = upper.indexOf(forbidden, idx + 1);
            }
        }
    }

    private static boolean isBoundary(String upper, int index, int length) {
        boolean beforeOk = (index == 0) || !Character.isLetterOrDigit(upper.charAt(index - 1));
        int end = index + length;
        boolean afterOk = (end >= upper.length()) || !Character.isLetterOrDigit(upper.charAt(end));
        return beforeOk && afterOk;
    }
}
