package com.example.demo.service;

import java.security.SecureRandom;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class PasswordUtil {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private PasswordUtil() {
    }

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        String stored = storedPassword.trim();

        if (stored.startsWith("$2")) {
            return ENCODER.matches(rawPassword, stored);
        }

        // 既存の初期データは pass などの平文で入っているため、移行用に平文比較も許可する。
        return stored.equals(rawPassword.trim());
    }

    public static String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            password.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return password.toString();
    }
}
