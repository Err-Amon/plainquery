package com.plainquery.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public final class EncodingDetector {

    private static final Logger LOG = Logger.getLogger(EncodingDetector.class.getName());

    private static final int BOM_READ_LIMIT = 4;

    private EncodingDetector() {}

    public static Charset detect(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return StandardCharsets.UTF_8;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bom = new byte[BOM_READ_LIMIT];
            int read = fis.read(bom, 0, BOM_READ_LIMIT);
            if (read < 2) {
                return StandardCharsets.UTF_8;
            }

            if (read >= 3
                    && (bom[0] & 0xFF) == 0xEF
                    && (bom[1] & 0xFF) == 0xBB
                    && (bom[2] & 0xFF) == 0xBF) {
                return StandardCharsets.UTF_8;
            }

            if (read >= 2
                    && (bom[0] & 0xFF) == 0xFE
                    && (bom[1] & 0xFF) == 0xFF) {
                return StandardCharsets.UTF_16BE;
            }

            if (read >= 2
                    && (bom[0] & 0xFF) == 0xFF
                    && (bom[1] & 0xFF) == 0xFE) {
                return StandardCharsets.UTF_16LE;
            }

            if (read >= 4
                    && bom[0] == 0x00
                    && bom[1] == 0x00
                    && (bom[2] & 0xFF) == 0xFE
                    && (bom[3] & 0xFF) == 0xFF) {
                return Charset.forName("UTF-32BE");
            }

            if (read >= 4
                    && (bom[0] & 0xFF) == 0xFF
                    && (bom[1] & 0xFF) == 0xFE
                    && bom[2] == 0x00
                    && bom[3] == 0x00) {
                return Charset.forName("UTF-32LE");
            }

        } catch (IOException e) {
            LOG.warning("Could not read BOM from file " + file.getName()
                + ": " + e.getMessage());
        }

        return StandardCharsets.ISO_8859_1;
    }
}
