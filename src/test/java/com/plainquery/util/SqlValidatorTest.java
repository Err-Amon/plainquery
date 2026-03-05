package com.plainquery.util;

import com.plainquery.exception.SqlValidationException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SqlValidatorTest {

    @Test
    void validSelectPasses() {
        assertDoesNotThrow(() ->
            SqlValidator.validate("SELECT id, name FROM users"));
    }

    @Test
    void validSelectWithWherePasses() {
        assertDoesNotThrow(() ->
            SqlValidator.validate("SELECT * FROM sales WHERE region = 'North'"));
    }

    @Test
    void validSelectWithGroupByPasses() {
        assertDoesNotThrow(() ->
            SqlValidator.validate("SELECT region, SUM(amount) FROM sales GROUP BY region"));
    }

    @Test
    void validSelectWithJoinPasses() {
        assertDoesNotThrow(() ->
            SqlValidator.validate("SELECT a.id, b.name FROM orders a JOIN customers b ON a.cid = b.id"));
    }

    @Test
    void validSelectWithSubqueryPasses() {
        assertDoesNotThrow(() ->
            SqlValidator.validate("SELECT * FROM (SELECT id, name FROM users WHERE active = 1)"));
    }

    @Test
    void leadingWhitespacePasses() {
        assertDoesNotThrow(() ->
            SqlValidator.validate("   SELECT 1"));
    }

    @Test
    void insertRejected() {
        SqlValidationException ex = assertThrows(SqlValidationException.class, () ->
            SqlValidator.validate("INSERT INTO users VALUES (1, 'test')"));
        assertTrue(ex.getMessage().contains("SELECT") || ex.getMessage().contains("INSERT"));
    }

    @Test
    void updateRejected() {
        assertThrows(SqlValidationException.class, () ->
            SqlValidator.validate("UPDATE users SET name = 'x' WHERE id = 1"));
    }

    @Test
    void deleteRejected() {
        assertThrows(SqlValidationException.class, () ->
            SqlValidator.validate("DELETE FROM users WHERE id = 1"));
    }

    @Test
    void dropRejected() {
        assertThrows(SqlValidationException.class, () ->
            SqlValidator.validate("DROP TABLE users"));
    }

    @Test
    void pragmaRejected() {
        assertThrows(SqlValidationException.class, () ->
            SqlValidator.validate("SELECT * FROM users; PRAGMA table_info(users)"));
    }

    @Test
    void attachRejected() {
        assertThrows(SqlValidationException.class, () ->
            SqlValidator.validate("ATTACH DATABASE 'other.db' AS other"));
    }

    @Test
    void semicolonRejected() {
        assertThrows(SqlValidationException.class, () ->
            SqlValidator.validate("SELECT 1; SELECT 2"));
    }

    @Test
    void emptyInputRejected() {
        assertThrows(SqlValidationException.class, () ->
            SqlValidator.validate(""));
    }

    @Test
    void whitespaceOnlyRejected() {
        assertThrows(SqlValidationException.class, () ->
            SqlValidator.validate("   "));
    }

    @Test
    void nullInputThrowsNullPointer() {
        assertThrows(NullPointerException.class, () ->
            SqlValidator.validate(null));
    }

    @Test
    void columnNamedCreatedAtPasses() {
        assertDoesNotThrow(() ->
            SqlValidator.validate("SELECT created_at FROM events"));
    }

    @Test
    void columnNamedUpdatedByPasses() {
        assertDoesNotThrow(() ->
            SqlValidator.validate("SELECT updated_by FROM audit_log"));
    }

    @Test
    void createRejected() {
        assertThrows(SqlValidationException.class, () ->
            SqlValidator.validate("CREATE TABLE foo (id INTEGER)"));
    }

    @Test
    void alterRejected() {
        assertThrows(SqlValidationException.class, () ->
            SqlValidator.validate("ALTER TABLE users ADD COLUMN email TEXT"));
    }
}
