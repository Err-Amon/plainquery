package com.plainquery.util;

import com.plainquery.model.ColumnDefinition;
import com.plainquery.model.ColumnType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SchemaInferer {

    private static final int MAX_SAMPLE_ROWS = 200;
    private static final int MAX_SAMPLE_VALUES = 5;

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("MM-dd-yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );

    private SchemaInferer() {}

        public static List<ColumnDefinition> infer(
            List<String> headers,
            List<String[]> rows) {

        Objects.requireNonNull(headers, "Headers must not be null");
        Objects.requireNonNull(rows, "Sample rows must not be null");

        int columnCount = headers.size();
        List<ColumnDefinition> definitions = new ArrayList<>(columnCount);

        List<String[]> bounded = rows.size() > MAX_SAMPLE_ROWS
            ? rows.subList(0, MAX_SAMPLE_ROWS)
            : rows;

        for (int col = 0; col < columnCount; col++) {
            String header = headers.get(col).trim();
            if (header.isEmpty()) {
                header = "column_" + (col + 1);
            }

            List<String> columnValues = extractColumnValues(bounded, col);
            ColumnType type = inferType(columnValues);
            List<String> samples = collectSamples(columnValues);

            definitions.add(new ColumnDefinition(header, type, samples));
        }

        return definitions;
    }

    private static List<String> extractColumnValues(List<String[]> rows, int colIndex) {
        List<String> values = new ArrayList<>();
        for (String[] row : rows) {
            if (colIndex < row.length) {
                String val = row[colIndex];
                if (val != null && !val.isBlank()) {
                    values.add(val.trim());
                }
            }
        }
        return values;
    }

    private static ColumnType inferType(List<String> values) {
        if (values.isEmpty()) {
            return ColumnType.TEXT;
        }

        boolean allInteger = true;
        boolean allReal = true;
        boolean allDate = true;

        for (String value : values) {
            if (allInteger && !isInteger(value)) {
                allInteger = false;
            }
            if (allReal && !isReal(value)) {
                allReal = false;
            }
            if (allDate && !isDate(value)) {
                allDate = false;
            }
            if (!allInteger && !allReal && !allDate) {
                break;
            }
        }

        if (allInteger) return ColumnType.INTEGER;
        if (allReal)    return ColumnType.REAL;
        if (allDate)    return ColumnType.DATE;
        return ColumnType.TEXT;
    }

    private static boolean isInteger(String value) {
        if (value == null || value.isEmpty()) return false;
        try {
            Long.parseLong(value.replace(",", "").trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isReal(String value) {
        if (value == null || value.isEmpty()) return false;
        try {
            Double.parseDouble(value.replace(",", "").trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDate(String value) {
        if (value == null || value.isEmpty()) return false;
        String trimmed = value.trim();
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                LocalDate.parse(trimmed, fmt);
                return true;
            } catch (DateTimeParseException e) {
                // try next format
            }
        }
        return false;
    }

    private static List<String> collectSamples(List<String> values) {
        List<String> samples = new ArrayList<>();
        for (String v : values) {
            if (!samples.contains(v)) {
                samples.add(v);
            }
            if (samples.size() >= MAX_SAMPLE_VALUES) {
                break;
            }
        }
        return samples;
    }

    
}
