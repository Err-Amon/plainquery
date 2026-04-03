package com.plainquery.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlSyntaxHighlighter {

    private static final String KEYWORD_PATTERN = "\\b(SELECT|FROM|WHERE|AND|OR|NOT|GROUP|BY|HAVING|ORDER|ASC|DESC|JOIN|LEFT|RIGHT|INNER|OUTER|ON|AS|DISTINCT|COUNT|SUM|AVG|MIN|MAX|CASE|WHEN|THEN|ELSE|END|LIKE|IN|BETWEEN|IS|NULL|TRUE|FALSE)\\b";
    private static final String FUNCTION_PATTERN = "\\b(COUNT|SUM|AVG|MIN|MAX|CONCAT|SUBSTR|DATE|TIME|NOW|CURRENT_DATE|CURRENT_TIME)\\b";
    private static final String OPERATOR_PATTERN = "(=|!=|<|>|<=|>=|\\+|-|\\*|\\/|\\|\\||&&|!)";
    private static final String NUMBER_PATTERN = "\\b(\\d+(\\.\\d*)?|\\.\\d+)\\b";
    private static final String STRING_PATTERN = "('([^'\\\\]|\\\\.)*')|(\"([^\"\\\\]|\\\\.)*\")";
    private static final String COMMENT_PATTERN = "(--.*$|/\\*[\\s\\S]*?\\*/)";

    private static final Pattern PATTERN = Pattern.compile(
        "(?:" + COMMENT_PATTERN + ")|" +
        "(?:" + STRING_PATTERN + ")|" +
        "(?:" + KEYWORD_PATTERN + ")|" +
        "(?:" + FUNCTION_PATTERN + ")|" +
        "(?:" + NUMBER_PATTERN + ")|" +
        "(?:" + OPERATOR_PATTERN + ")",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    private SqlSyntaxHighlighter() {}


    public static String formatSql(String sql) {
        if (sql == null || sql.isEmpty()) return sql;

        // Simple SQL formatter for readability
        return sql
            .replaceAll("\\s+", " ")
            .trim()
            .replaceAll("\\bSELECT\\b", "\nSELECT")
            .replaceAll("\\bFROM\\b", "\nFROM")
            .replaceAll("\\bWHERE\\b", "\nWHERE")
            .replaceAll("\\bGROUP BY\\b", "\nGROUP BY")
            .replaceAll("\\bHAVING\\b", "\nHAVING")
            .replaceAll("\\bORDER BY\\b", "\nORDER BY")
            .replaceAll("\\bJOIN\\b", "\nJOIN")
            .replaceAll("\\bLEFT JOIN\\b", "\nLEFT JOIN")
            .replaceAll("\\bRIGHT JOIN\\b", "\nRIGHT JOIN")
            .replaceAll("\\bINNER JOIN\\b", "\nINNER JOIN")
            .replaceAll("\\bON\\b", "\n  ON")
            .replaceAll("\\bAND\\b", "\n  AND")
            .replaceAll("\\bOR\\b", "\n  OR")
            .replaceAll("\\bCASE\\b", "\nCASE")
            .replaceAll("\\bWHEN\\b", "\n  WHEN")
            .replaceAll("\\bTHEN\\b", "\n    THEN")
            .replaceAll("\\bELSE\\b", "\n  ELSE")
            .replaceAll("\\bEND\\b", "\nEND")
            .trim();
    }
}
