package com.plainquery.util;

import com.plainquery.model.ColumnDefinition;
import com.plainquery.model.ColumnType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SchemaInfererTest {

    @Test
    void testInferIntegerColumns() {
        List<String> headers = Arrays.asList("id", "age", "count");
        List<String[]> rows = Arrays.asList(
            new String[]{"1", "30", "100"},
            new String[]{"2", "25", "50"},
            new String[]{"3", "35", "75"}
        );

        List<ColumnDefinition> schema = SchemaInferer.infer(headers, rows);
        
        assertNotNull(schema);
        assertEquals(3, schema.size());
        
        assertEquals("id", schema.get(0).getName());
        assertEquals(ColumnType.INTEGER, schema.get(0).getColumnType());
        
        assertEquals("age", schema.get(1).getName());
        assertEquals(ColumnType.INTEGER, schema.get(1).getColumnType());
        
        assertEquals("count", schema.get(2).getName());
        assertEquals(ColumnType.INTEGER, schema.get(2).getColumnType());
    }

    @Test
    void testInferRealColumns() {
        List<String> headers = Arrays.asList("price", "weight", "temperature");
        List<String[]> rows = Arrays.asList(
            new String[]{"19.99", "2.5", "25.5"},
            new String[]{"29.99", "3.2", "30.0"},
            new String[]{"9.99", "1.8", "18.5"}
        );

        List<ColumnDefinition> schema = SchemaInferer.infer(headers, rows);
        
        assertNotNull(schema);
        assertEquals(3, schema.size());
        
        assertEquals(ColumnType.REAL, schema.get(0).getColumnType());
        assertEquals(ColumnType.REAL, schema.get(1).getColumnType());
        assertEquals(ColumnType.REAL, schema.get(2).getColumnType());
    }

    @Test
    void testInferDateColumns() {
        List<String> headers = Arrays.asList("start_date", "end_date", "birth_date");
        List<String[]> rows = Arrays.asList(
            new String[]{"2023-10-01", "2023-12-31", "1990-05-15"},
            new String[]{"2024-01-15", "2024-03-20", "1985-11-03"},
            new String[]{"2023-07-04", "2023-08-15", "1995-03-22"}
        );

        List<ColumnDefinition> schema = SchemaInferer.infer(headers, rows);
        
        assertNotNull(schema);
        assertEquals(3, schema.size());
        
        assertEquals(ColumnType.DATE, schema.get(0).getColumnType());
        assertEquals(ColumnType.DATE, schema.get(1).getColumnType());
        assertEquals(ColumnType.DATE, schema.get(2).getColumnType());
    }

    @Test
    void testInferTextColumns() {
        List<String> headers = Arrays.asList("name", "email", "city");
        List<String[]> rows = Arrays.asList(
            new String[]{"John Doe", "john@example.com", "New York"},
            new String[]{"Jane Smith", "jane@example.com", "London"},
            new String[]{"Bob Johnson", "bob@example.com", "Paris"}
        );

        List<ColumnDefinition> schema = SchemaInferer.infer(headers, rows);
        
        assertNotNull(schema);
        assertEquals(3, schema.size());
        
        assertEquals(ColumnType.TEXT, schema.get(0).getColumnType());
        assertEquals(ColumnType.TEXT, schema.get(1).getColumnType());
        assertEquals(ColumnType.TEXT, schema.get(2).getColumnType());
    }

    @Test
    void testInferMixedTypes() {
        List<String> headers = Arrays.asList("id", "name", "price", "birth_date");
        List<String[]> rows = Arrays.asList(
            new String[]{"1", "John", "19.99", "1990-05-15"},
            new String[]{"2", "Jane", "29.99", "1985-11-03"},
            new String[]{"3", "Bob", "9.99", "1995-03-22"}
        );

        List<ColumnDefinition> schema = SchemaInferer.infer(headers, rows);
        
        assertNotNull(schema);
        assertEquals(4, schema.size());
        
        assertEquals(ColumnType.INTEGER, schema.get(0).getColumnType());
        assertEquals(ColumnType.TEXT, schema.get(1).getColumnType());
        assertEquals(ColumnType.REAL, schema.get(2).getColumnType());
        assertEquals(ColumnType.DATE, schema.get(3).getColumnType());
    }

    @Test
    void testInferEmptyHeaders() {
        List<String> headers = Arrays.asList("", " ", "   ");
        List<String[]> rows = Arrays.<String[]>asList(
            new String[]{"1", "2", "3"}
        );

        List<ColumnDefinition> schema = SchemaInferer.infer(headers, rows);
        
        assertEquals("column_1", schema.get(0).getName());
        assertEquals("column_2", schema.get(1).getName());
        assertEquals("column_3", schema.get(2).getName());
    }

    @Test
    void testInferEmptyRows() {
        List<String> headers = Arrays.asList("id", "name", "age");
        List<String[]> rows = Collections.emptyList();

        List<ColumnDefinition> schema = SchemaInferer.infer(headers, rows);
        
        assertNotNull(schema);
        assertEquals(3, schema.size());
        assertEquals(ColumnType.TEXT, schema.get(0).getColumnType());
        assertEquals(ColumnType.TEXT, schema.get(1).getColumnType());
        assertEquals(ColumnType.TEXT, schema.get(2).getColumnType());
    }
}
