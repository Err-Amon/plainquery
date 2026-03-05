package com.plainquery.util;

import com.plainquery.exception.SqlExtractionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlExtractorTest {

    @Test
    void testExtractSingleQuery() throws SqlExtractionException {
        String input = "Here's your SQL query:\n```sql\nSELECT * FROM users;\n```\nEnjoy your results!";
        String result = SqlExtractor.extract(input);
        assertEquals("SELECT * FROM users;", result.trim(), "Should extract SQL from markdown code block");
    }

    @Test
    void testExtractWithoutCodeBlock() throws SqlExtractionException {
        String input = "Please execute: SELECT * FROM users;";
        String result = SqlExtractor.extract(input);
        assertEquals("SELECT * FROM users;", result.trim(), "Should extract SQL without code block");
    }

    @Test
    void testExtractWithMultipleQueries() throws SqlExtractionException {
        String input = "First query: SELECT * FROM users;\nSecond query: SELECT * FROM orders;";
        String result = SqlExtractor.extract(input);
        assertEquals("SELECT * FROM users;", result.trim(), "Should extract first SQL query");
    }

    @Test
    void testExtractWithBackticks() throws SqlExtractionException {
        String input = "```\nSELECT * FROM users;\n```";
        String result = SqlExtractor.extract(input);
        assertEquals("SELECT * FROM users;", result.trim(), "Should extract SQL from generic code block");
    }

    @Test
    void testExtractNoSql() {
        assertThrows(SqlExtractionException.class, () -> SqlExtractor.extract("No SQL here"), 
            "Should throw exception when no SQL found");
    }

    @Test
    void testExtractEmptyString() {
        assertThrows(SqlExtractionException.class, () -> SqlExtractor.extract(""), 
            "Should throw exception for empty string");
    }

    @Test
    void testExtractNull() {
        assertThrows(SqlExtractionException.class, () -> SqlExtractor.extract(null), 
            "Should throw exception for null input");
    }

    @Test
    void testExtractComplexQuery() throws SqlExtractionException {
        String input = "```sql\nSELECT u.name, o.amount \nFROM users u \nJOIN orders o ON u.id = o.user_id \nWHERE o.amount > 100 \nORDER BY o.date DESC;\n```";
        String result = SqlExtractor.extract(input);
        assertEquals("SELECT u.name, o.amount FROM users u JOIN orders o ON u.id = o.user_id WHERE o.amount > 100 ORDER BY o.date DESC;", 
            result.replaceAll("\\s+", " ").trim(), 
            "Should extract complex SQL with newlines");
    }
}
