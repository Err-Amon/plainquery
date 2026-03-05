package com.plainquery.util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DelimiterDetectorTest {

    @Test
    void testDetectCommaDelimiter() throws IOException {
        Path tempFile = Files.createTempFile("test", ".csv");
        String csvContent = "name,age,city\nJohn,30,New York\nJane,25,London";
        Files.write(tempFile, csvContent.getBytes(StandardCharsets.UTF_8));
        
        char detected = DelimiterDetector.detect(tempFile.toFile(), StandardCharsets.UTF_8);
        assertEquals(',', detected, "Should detect comma as delimiter");
        
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testDetectTabDelimiter() throws IOException {
        Path tempFile = Files.createTempFile("test", ".csv");
        String csvContent = "name\tage\tcity\nJohn\t30\tNew York\nJane\t25\tLondon";
        Files.write(tempFile, csvContent.getBytes(StandardCharsets.UTF_8));
        
        char detected = DelimiterDetector.detect(tempFile.toFile(), StandardCharsets.UTF_8);
        assertEquals('\t', detected, "Should detect tab as delimiter");
        
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testDetectPipeDelimiter() throws IOException {
        Path tempFile = Files.createTempFile("test", ".csv");
        String csvContent = "name|age|city\nJohn|30|New York\nJane|25|London";
        Files.write(tempFile, csvContent.getBytes(StandardCharsets.UTF_8));
        
        char detected = DelimiterDetector.detect(tempFile.toFile(), StandardCharsets.UTF_8);
        assertEquals('|', detected, "Should detect pipe as delimiter");
        
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testDetectSemicolonDelimiter() throws IOException {
        Path tempFile = Files.createTempFile("test", ".csv");
        String csvContent = "name;age;city\nJohn;30;New York\nJane;25;London";
        Files.write(tempFile, csvContent.getBytes(StandardCharsets.UTF_8));
        
        char detected = DelimiterDetector.detect(tempFile.toFile(), StandardCharsets.UTF_8);
        assertEquals(';', detected, "Should detect semicolon as delimiter");
        
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testDetectEmptyFile() throws IOException {
        Path tempFile = Files.createTempFile("test", ".csv");
        Files.write(tempFile, new byte[0]);
        
        char detected = DelimiterDetector.detect(tempFile.toFile(), StandardCharsets.UTF_8);
        assertEquals(',', detected, "Should return default comma for empty file");
        
        Files.deleteIfExists(tempFile);
    }
}
