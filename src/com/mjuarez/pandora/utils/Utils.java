package com.mjuarez.pandora.utils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.CRC32;

public class Utils {
    public static List<String> getTokens(String inputLine, char delimitedCharacter) {
        return Arrays.asList(inputLine.split(Character.toString(delimitedCharacter)));
    }

    public static void log(String logMessage) {
        log(logMessage, null);
    }

    public static void log(String logMessage, Exception exception) {
        String exceptionMessage = (exception != null ? " - Exception: " + exception.getLocalizedMessage() : "");

        // SimpleDateFormat is not thread safe, so need to instantiate one every time we use it.
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis()));
        System.out.println(date + " - " + logMessage + exceptionMessage);
    }

    public static long crc32Hash(String string, int modNumber) {
        CRC32 crc32 = new CRC32();
        crc32.update(string.getBytes());
        return crc32.getValue() % modNumber;
    }

    public static long crc32MultiHash(String string, int hashes) {
        CRC32 crc32 = new CRC32();
        crc32.update(string.getBytes());
        crc32.update(hashes * 31);
        return crc32.getValue();
    }
}
