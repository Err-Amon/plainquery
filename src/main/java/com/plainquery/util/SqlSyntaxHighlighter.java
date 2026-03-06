package com.plainquery.util;

import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

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


    public static void highlightSql(TextArea textArea, String sql) {
        if (textArea == null || sql == null) return;

        Matcher matcher = PATTERN.matcher(sql);
        int lastIndex = 0;
        TextFlow textFlow = new TextFlow();

        while (matcher.find()) {
            // Add non-matching text
            if (matcher.start() > lastIndex) {
                String plainText = sql.substring(lastIndex, matcher.start());
                Text plain = new Text(plainText);
                plain.getStyleClass().add("sql-plain");
                textFlow.getChildren().add(plain);
            }

            // Add matching text with appropriate style
            String matchedText = matcher.group();
            Text text = new Text(matchedText);

            if (matchedText.startsWith("--") || matchedText.startsWith("/*")) {
                text.getStyleClass().add("sql-comment");
            } else if (matchedText.startsWith("'") || matchedText.startsWith("\"")) {
                text.getStyleClass().add("sql-string");
            } else if (isKeyword(matchedText)) {
                text.getStyleClass().add("sql-keyword");
            } else if (isFunction(matchedText)) {
                text.getStyleClass().add("sql-function");
            } else if (isNumber(matchedText)) {
                text.getStyleClass().add("sql-number");
            } else if (isOperator(matchedText)) {
                text.getStyleClass().add("sql-operator");
            }

            textFlow.getChildren().add(text);
            lastIndex = matcher.end();
        }

        // Add remaining text
        if (lastIndex < sql.length()) {
            String plainText = sql.substring(lastIndex);
            Text plain = new Text(plainText);
            plain.getStyleClass().add("sql-plain");
            textFlow.getChildren().add(plain);
        }
    }

    private static boolean isKeyword(String text) {
        return Pattern.compile(KEYWORD_PATTERN, Pattern.CASE_INSENSITIVE).matcher(text).matches();
    }

    private static boolean isFunction(String text) {
        return Pattern.compile(FUNCTION_PATTERN, Pattern.CASE_INSENSITIVE).matcher(text).matches();
    }

    private static boolean isNumber(String text) {
        return Pattern.compile(NUMBER_PATTERN).matcher(text).matches();
    }

    private static boolean isOperator(String text) {
        return Pattern.compile(OPERATOR_PATTERN).matcher(text).matches();
    }


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
