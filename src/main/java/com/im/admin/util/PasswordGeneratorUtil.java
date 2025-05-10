package com.im.admin.util;

import java.security.SecureRandom;

public class PasswordGeneratorUtil {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_";

    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomPassword(int length) {
        StringBuilder password = new StringBuilder(length);

        // Ensure at least one of each required type
        password.append(randomChar(UPPER));
        password.append(randomChar(LOWER));
        password.append(randomChar(DIGITS));
        password.append(randomChar(SPECIAL));

        // Fill the rest randomly
        for (int i = 4; i < length; i++) {
            password.append(randomChar(ALL));
        }

        // Shuffle to avoid predictable patterns
        return shuffleString(password.toString());
    }

    private static char randomChar(String input) {
        return input.charAt(RANDOM.nextInt(input.length()));
    }

    private static String shuffleString(String input) {
        char[] a = input.toCharArray();
        for (int i = a.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = a[i];
            a[i] = a[j];
            a[j] = temp;
        }
        return new String(a);
    }
}

