package com.plainquery.util;

import com.plainquery.model.ColumnDefinition;
import com.plainquery.model.TableSchema;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class SqlIdentifierQuoter {

    private SqlIdentifierQuoter() {}

    public static String quoteIdentifiers(String sql, List<TableSchema> schemas) {
        Objects.requireNonNull(sql, "SQL must not be null");
        if (schemas == null || schemas.isEmpty()) return sql;

        List<Identifier> ids = new ArrayList<>();
        for (TableSchema t : schemas) {
            String tname = t.getTableName();
            if (needsQuoting(tname)) ids.add(new Identifier(tname));
            for (ColumnDefinition c : t.getColumns()) {
                String cname = c.getName();
                if (needsQuoting(cname)) ids.add(new Identifier(cname));
            }
        }

        if (ids.isEmpty()) return sql;

        // Sort by length desc to match longest first
        ids.sort(Comparator.comparingInt((Identifier id) -> id.name.length()).reversed());

        StringBuilder out = new StringBuilder(sql.length() + 32);
        String lower = sql.toLowerCase(Locale.ROOT);
        int i = 0;
        int n = sql.length();
        while (i < n) {
            char ch = sql.charAt(i);
            if (ch == '\'') {
                // copy quoted string as-is (handle doubled single quotes)
                int start = i;
                out.append(ch);
                i++;
                while (i < n) {
                    char c2 = sql.charAt(i);
                    out.append(c2);
                    i++;
                    if (c2 == '\'') {
                        // end or escaped quote (two single quotes)
                        // if next is also quote, continue
                        if (i < n && sql.charAt(i) == '\'') {
                            out.append('\'');
                            i++;
                            continue;
                        }
                        break;
                    }
                }
                continue;
            }

            boolean matched = false;
            for (Identifier id : ids) {
                int len = id.name.length();
                if (i + len <= n) {
                    // region match ignoring case
                    if (lower.regionMatches(i, id.nameLower, 0, len)) {
                        // ensure not already quoted
                        boolean precededByQuote = i > 0 && sql.charAt(i - 1) == '"';
                        boolean followedByQuote = (i + len) < n && sql.charAt(i + len) == '"';
                        if (!precededByQuote && !followedByQuote) {
                            // ensure boundary: previous and next char are not identifier chars
                            boolean okBefore = i == 0 || !isIdentifierChar(sql.charAt(i - 1));
                            boolean okAfter = (i + len) == n || !isIdentifierChar(sql.charAt(i + len));
                            if (okBefore && okAfter) {
                                out.append('"').append(id.escaped).append('"');
                                i += len;
                                matched = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (!matched) {
                out.append(ch);
                i++;
            }
        }

        return out.toString();
    }

    private static boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static boolean needsQuoting(String name) {
        if (name == null || name.isBlank()) return false;
        // Needs quoting if contains any character outside [A-Za-z0-9_]
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_')) return true;
        }
        // also if starts with digit
        char first = name.charAt(0);
        return Character.isDigit(first);
    }

    private static final class Identifier {
        final String name;
        final String nameLower;
        final String escaped;

        Identifier(String name) {
            this.name = name;
            this.nameLower = name.toLowerCase(Locale.ROOT);
            this.escaped = name.replace("\"", "\"\"");
        }
    }
}
