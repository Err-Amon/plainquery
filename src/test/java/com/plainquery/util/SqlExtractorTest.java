package com.plainquery.util;

import com.plainquery.exception.SqlExtractionException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SqlExtractorTest {

    @Test
    void extractFromSqlCodeBlock() throws SqlExtractionException {
        String response = "Here is the SQL:\n```sql\nSELECT id, name FROM users\n```";
        String result = SqlExtractor.extract(response);
        assertEquals("SELECT id, name FROM users", result);
    }

    @Test
    void extractFromGenericCodeBlock() throws SqlExtractionException {
        String response = "```\nSELECT * FROM orders WHERE total > 100\n```";
        String result = SqlExtractor.extract(response);
        assertEquals("SELECT * FROM orders WHERE total > 100", result);
    }

    @Test
    void extractFromPlainTextWithSelect() throws SqlExtractionException {
        String response = "The query you need is: SELECT region, SUM(amount) FROM sales GROUP BY region";
        String result = SqlExtractor.extract(response);
        assertTrue(result.toUpperCase().startsWith("SELECT"));
        assertTrue(result.contains("sales"));
    }

    @Test
    void extractWhenResponseIsJustSelect() throws SqlExtractionException {
        String response = "SELECT name FROM products WHERE price < 50";
        String result = SqlExtractor.extract(response);
        assertEquals("SELECT name FROM products WHERE price < 50", result);
    }

    @Test
    void extractRemovesSemicolon() throws SqlExtractionException {
        String response = "```sql\nSELECT * FROM users;\n```";
        String result = SqlExtractor.extract(response);
        assertFalse(result.contains(";"));
    }

    @Test
    void extractWithExplanationPrefix() throws SqlExtractionException {
        String response = "To answer this question, you can use:\n"
            + "SELECT department, COUNT(*) AS headcount FROM employees GROUP BY department";
        String result = SqlExtractor.extract(response);
        assertTrue(result.toUpperCase().startsWith("SELECT"));
    }

    @Test
    void noSqlThrowsException() {
        assertThrows(SqlExtractionException.class, () ->
            SqlExtractor.extract("I cannot generate SQL for this question."));
    }

    @Test
    void emptyResponseThrowsException() {
        assertThrows(SqlExtractionException.class, () ->
            SqlExtractor.extract(""));
    }

    @Test
    void nullResponseThrowsNullPointer() {
        assertThrows(NullPointerException.class, () ->
            SqlExtractor.extract(null));
    }

    @Test
    void codeBlockWithUpperCaseSqlKeyword() throws SqlExtractionException {
        String response = "```SQL\nSELECT COUNT(*) FROM logs\n```";
        String result = SqlExtractor.extract(response);
        assertTrue(result.toUpperCase().startsWith("SELECT"));
    }

    @Test
    void multipleCodeBlocksTakesLast() throws SqlExtractionException {
        String response = "```sql\nSELECT 1\n```\n"
            + "Actually, use this:\n"
            + "```sql\nSELECT id FROM users WHERE active = 1\n```";
        String result = SqlExtractor.extract(response);
        assertTrue(result.contains("active"));
    }
}
