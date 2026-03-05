package com.plainquery.util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncodingDetectorTest {

    @Test
    void testDetectUTF8Encoding() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");
        String content = "Hello, world! こんにちは世界";
        Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));
        
        Charset detected = EncodingDetector.detect(tempFile.toFile());
        assertEquals(StandardCharsets.UTF_8, detected, "Should detect UTF-8 encoding");
        
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testDetectUTF16Encoding() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");
        String content = "Hello, world! こんにちは世界";
        Files.write(tempFile, content.getBytes(StandardCharsets.UTF_16));
        
        Charset detected = EncodingDetector.detect(tempFile.toFile());
        assertEquals(StandardCharsets.UTF_16, detected, "Should detect UTF-16 encoding");
        
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testDetectISO88591Encoding() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");
        String content = "Hello, world! Àéîøü";
        Files.write(tempFile, content.getBytes(StandardCharsets.ISO_8859_1));
        
        Charset detected = EncodingDetector.detect(tempFile.toFile());
        assertEquals(StandardCharsets.ISO_8859_1, detected, "Should detect ISO-8859-1 encoding");
        
        Files.deleteIfExists(tempFile);
    }
}
