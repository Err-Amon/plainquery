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
        byte[] utf8WithBom = new byte[content.getBytes(StandardCharsets.UTF_8).length + 3];
        utf8WithBom[0] = (byte) 0xEF;
        utf8WithBom[1] = (byte) 0xBB;
        utf8WithBom[2] = (byte) 0xBF;
        System.arraycopy(content.getBytes(StandardCharsets.UTF_8), 0, utf8WithBom, 3, content.getBytes(StandardCharsets.UTF_8).length);
        Files.write(tempFile, utf8WithBom);
        
        Charset detected = EncodingDetector.detect(tempFile.toFile());
        assertEquals(StandardCharsets.UTF_8, detected, "Should detect UTF-8 encoding with BOM");
        
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testDetectUTF16BEEncoding() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");
        String content = "Hello, world! こんにちは世界";
        Files.write(tempFile, content.getBytes(StandardCharsets.UTF_16BE));
        
        Charset detected = EncodingDetector.detect(tempFile.toFile());
        assertEquals(StandardCharsets.UTF_16BE, detected, "Should detect UTF-16BE encoding");
        
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testDetectUTF16LEEncoding() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");
        String content = "Hello, world! こんにちは世界";
        Files.write(tempFile, content.getBytes(StandardCharsets.UTF_16LE));
        
        Charset detected = EncodingDetector.detect(tempFile.toFile());
        assertEquals(StandardCharsets.UTF_16LE, detected, "Should detect UTF-16LE encoding");
        
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
