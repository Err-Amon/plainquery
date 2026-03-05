package com.plainquery.util;

import com.plainquery.exception.SqlValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlValidatorTest {

    @Test
    void testValidSelectQuery() {
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT * FROM users"), 
            "Valid SELECT query should not throw exception");
    }

    @Test
    void testValidSelectWithWhere() {
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT name, age FROM users WHERE age > 18"), 
            "Valid SELECT with WHERE should not throw exception");
    }

    @Test
    void testInvalidUpdateQuery() {
        assertThrows(SqlValidationException.class, () -> SqlValidator.validate("UPDATE users SET name = 'test'"), 
            "UPDATE query should throw validation exception");
    }

    @Test
    void testInvalidDeleteQuery() {
        assertThrows(SqlValidationException.class, () -> SqlValidator.validate("DELETE FROM users"), 
            "DELETE query should throw validation exception");
    }

    @Test
    void testInvalidInsertQuery() {
        assertThrows(SqlValidationException.class, () -> SqlValidator.validate("INSERT INTO users VALUES (1, 'test')"), 
            "INSERT query should throw validation exception");
    }

    @Test
    void testInvalidCreateQuery() {
        assertThrows(SqlValidationException.class, () -> SqlValidator.validate("CREATE TABLE test (id INT)"), 
            "CREATE TABLE query should throw validation exception");
    }

    @Test
    void testInvalidDropQuery() {
        assertThrows(SqlValidationException.class, () -> SqlValidator.validate("DROP TABLE users"), 
            "DROP TABLE query should throw validation exception");
    }

    @Test
    void testEmptyQuery() {
        assertThrows(SqlValidationException.class, () -> SqlValidator.validate(""), 
            "Empty query should throw validation exception");
    }

    @Test
    void testNullQuery() {
        assertThrows(SqlValidationException.class, () -> SqlValidator.validate(null), 
            "Null query should throw validation exception");
    }

    @Test
    void testQueryWithLeadingWhitespace() {
        assertDoesNotThrow(() -> SqlValidator.validate("   SELECT * FROM users   "), 
            "Query with leading/trailing whitespace should be valid");
    }
}
