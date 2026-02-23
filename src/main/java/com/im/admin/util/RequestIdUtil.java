package com.im.admin.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class RequestIdUtil {

    public static String generate() {

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder randomStr = new StringBuilder();

        for (int i = 0; i < 27; i++) {
            randomStr.append(chars.charAt(new Random().nextInt(chars.length())));
        }

        LocalDateTime now = LocalDateTime.now();

        int yearDigit = now.getYear() % 10;
        int dayOfYear = now.getDayOfYear();

        String time = now.format(DateTimeFormatter.ofPattern("HHmm"));

        return randomStr + "" +
                yearDigit +
                String.format("%03d", dayOfYear) +
                time;
    }
}
