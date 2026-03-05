package com.plainquery.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableNameSanitizerTest {

    @Test
    void testSanitizeValidTableName() {
        String result = TableNameSanitizer.sanitize("valid_table_name");
        assertEquals("valid_table_name", result, "Should leave valid table name unchanged");
    }

    @Test
    void testSanitizeWithSpecialCharacters() {
        String result = TableNameSanitizer.sanitize("table@name#123");
        assertEquals("table_name_123", result, "Should replace special characters with underscores");
    }

    @Test
    void testSanitizeWithSpaces() {
        String result = TableNameSanitizer.sanitize("table name with spaces");
        assertEquals("table_name_with_spaces", result, "Should replace spaces with underscores");
    }

    @Test
    void testSanitizeWithLeadingNumbers() {
        String result = TableNameSanitizer.sanitize("123table");
        assertEquals("_123table", result, "Should prefix table names starting with numbers");
    }

    @Test
    void testSanitizeWithEmptyString() {
        String result = TableNameSanitizer.sanitize("");
        assertEquals("_", result, "Should handle empty string");
    }

    @Test
    void testSanitizeNull() {
        String result = TableNameSanitizer.sanitize(null);
        assertEquals("_", result, "Should handle null input");
    }
}
