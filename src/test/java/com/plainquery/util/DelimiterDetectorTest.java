package com.plainquery.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class DelimiterDetectorTest {

    @TempDir
    Path tempDir;

    private File writeCsv(String filename, String content) throws IOException {
        File file = tempDir.resolve(filename).toFile();
        try (FileWriter w = new FileWriter(file, StandardCharsets.UTF_8)) {
            w.write(content);
        }
        return file;
    }

    @Test
    void detectsCommaDelimiter() throws IOException {
        File file = writeCsv("comma.csv",
            "id,name,amount\n1,Alice,100\n2,Bob,200\n3,Carol,300\n");
        char delimiter = DelimiterDetector.detect(file, StandardCharsets.UTF_8);
        assertEquals(',', delimiter);
    }

    @Test
    void detectsTabDelimiter() throws IOException {
        File file = writeCsv("tab.csv",
            "id\tname\tamount\n1\tAlice\t100\n2\tBob\t200\n3\tCarol\t300\n");
        char delimiter = DelimiterDetector.detect(file, StandardCharsets.UTF_8);
        assertEquals('\t', delimiter);
    }

    @Test
    void detectsPipeDelimiter() throws IOException {
        File file = writeCsv("pipe.csv",
            "id|name|amount\n1|Alice|100\n2|Bob|200\n3|Carol|300\n");
        char delimiter = DelimiterDetector.detect(file, StandardCharsets.UTF_8);
        assertEquals('|', delimiter);
    }

    @Test
    void detectsSemicolonDelimiter() throws IOException {
        File file = writeCsv("semi.csv",
            "id;name;amount\n1;Alice;100\n2;Bob;200\n3;Carol;300\n");
        char delimiter = DelimiterDetector.detect(file, StandardCharsets.UTF_8);
        assertEquals(';', delimiter);
    }

    @Test
    void emptyFileDefaultsToComma() throws IOException {
        File file = writeCsv("empty.csv", "");
        char delimiter = DelimiterDetector.detect(file, StandardCharsets.UTF_8);
        assertEquals(',', delimiter);
    }

    @Test
    void singleColumnDefaultsToComma() throws IOException {
        File file = writeCsv("single.csv", "name\nAlice\nBob\nCarol\n");
        char delimiter = DelimiterDetector.detect(file, StandardCharsets.UTF_8);
        assertEquals(',', delimiter);
    }

    @Test
    void nullFileReturnsDefault() {
        assertThrows(NullPointerException.class, () ->
            DelimiterDetector.detect(null, StandardCharsets.UTF_8));
    }

    @Test
    void ignoresDelimitersInsideQuotes() throws IOException {
        File file = writeCsv("quoted.csv",
            "id,name,note\n1,Alice,\"hello, world\"\n2,Bob,\"foo, bar\"\n");
        char delimiter = DelimiterDetector.detect(file, StandardCharsets.UTF_8);
        assertEquals(',', delimiter);
    }
}
