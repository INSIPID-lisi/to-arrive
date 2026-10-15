package com.insipidknight.toarrive.common.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GenerateUtil {
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int ORDER_NO_RANDOM_BOUND = 10000;

    public static String generateUid() {
        StringBuilder sb = new StringBuilder(LENGTH);
            for (int i = 0; i < LENGTH; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            return sb.toString();
        }

    public static String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        return "TA" + timestamp + String.format("%04d", RANDOM.nextInt(ORDER_NO_RANDOM_BOUND));
    }

}
