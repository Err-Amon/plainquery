package com.plainquery.util;

import com.plainquery.model.ColumnDefinition;
import com.plainquery.model.ColumnType;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaInfererTest {

    private List<ColumnDefinition> infer(String[] headers, String[]... rows) {
        return SchemaInferer.infer(List.of(headers), List.of(rows));
    }

    @Test
    void allIntegerColumnInferredAsInteger() {
        List<ColumnDefinition> defs = infer(
            new String[]{"id"},
            new String[]{"1"},
            new String[]{"2"},
            new String[]{"3"}
        );
        assertEquals(ColumnType.INTEGER, defs.get(0).getColumnType());
    }

    @Test
    void mixedIntAndFloatInferredAsReal() {
        List<ColumnDefinition> defs = infer(
            new String[]{"price"},
            new String[]{"10"},
            new String[]{"20.5"},
            new String[]{"30"}
        );
        assertEquals(ColumnType.REAL, defs.get(0).getColumnType());
    }

    @Test
    void isoDateStringsInferredAsDate() {
        List<ColumnDefinition> defs = infer(
            new String[]{"created_at"},
            new String[]{"2023-01-15"},
            new String[]{"2023-06-30"},
            new String[]{"2024-12-01"}
        );
        assertEquals(ColumnType.DATE, defs.get(0).getColumnType());
    }

    @Test
    void mixedTextInferredAsText() {
        List<ColumnDefinition> defs = infer(
            new String[]{"region"},
            new String[]{"North"},
            new String[]{"South"},
            new String[]{"East"}
        );
        assertEquals(ColumnType.TEXT, defs.get(0).getColumnType());
    }

    @Test
    void emptyColumnDefaultsToText() {
        List<ColumnDefinition> defs = infer(
            new String[]{"notes"},
            new String[]{""},
            new String[]{""},
            new String[]{""}
        );
        assertEquals(ColumnType.TEXT, defs.get(0).getColumnType());
    }

    @Test
    void multipleColumnsInferredIndependently() {
        List<ColumnDefinition> defs = infer(
            new String[]{"id", "name", "amount", "date"},
            new String[]{"1", "Alice", "100.5", "2023-01-01"},
            new String[]{"2", "Bob",   "200.0", "2023-02-01"}
        );
        assertEquals(4, defs.size());
        assertEquals(ColumnType.INTEGER, defs.get(0).getColumnType());
        assertEquals(ColumnType.TEXT,    defs.get(1).getColumnType());
        assertEquals(ColumnType.REAL,    defs.get(2).getColumnType());
        assertEquals(ColumnType.DATE,    defs.get(3).getColumnType());
    }

    @Test
    void blankHeaderReplacedWithColumnN() {
        List<ColumnDefinition> defs = infer(
            new String[]{"", "name"},
            new String[]{"1", "Alice"}
        );
        assertEquals("column_1", defs.get(0).getName());
        assertEquals("name",     defs.get(1).getName());
    }

    @Test
    void sampleValuesCollected() {
        List<ColumnDefinition> defs = infer(
            new String[]{"region"},
            new String[]{"North"},
            new String[]{"South"},
            new String[]{"East"},
            new String[]{"North"}
        );
        List<String> samples = defs.get(0).getSampleValues();
        assertFalse(samples.isEmpty());
        assertTrue(samples.contains("North"));
        assertTrue(samples.contains("South"));
    }

    @Test
    void formattedNumbersWithCommasInferredAsReal() {
        List<ColumnDefinition> defs = infer(
            new String[]{"revenue"},
            new String[]{"1,000.00"},
            new String[]{"2,500.50"},
            new String[]{"3,200.75"}
        );
        assertEquals(ColumnType.REAL, defs.get(0).getColumnType());
    }

    @Test
    void noRowsDefaultsToText() {
        List<ColumnDefinition> defs = SchemaInferer.infer(
            List.of("id", "name"),
            List.of()
        );
        assertEquals(ColumnType.TEXT, defs.get(0).getColumnType());
        assertEquals(ColumnType.TEXT, defs.get(1).getColumnType());
    }
}
