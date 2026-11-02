package com.movieticket.util;

import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Unit 1: Java Fundamentals - Classes, Static Members, Arrays, Loops, and String Handling.
 * Pure Java salted password hashing without external cryptography packages.
 */
@Component
public class PasswordEncoder {

    private static final int SALT_LENGTH = 16;
    private static final int HASH_ROUNDS = 64;
    private static final char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private static final char[] TOKEN_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'
    };
    private static final Random TOKEN_RANDOM = new Random();

    private final Random random = new Random();

    /**
     * Generates a random uppercase hexadecimal token of the requested length.
     */
    public static synchronized String generateToken(int length) {
        int safeLen = length > 0 ? length : 8;
        char[] out = new char[safeLen];
        for (int i = 0; i < safeLen; i++) {
            out[i] = TOKEN_CHARS[TOKEN_RANDOM.nextInt(TOKEN_CHARS.length)];
        }
        return new String(out);
    }

    /**
     * Encodes a raw password with a random hexadecimal salt.
     * Format: <saltHex>:<hashHex>
     */
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Raw password cannot be null");
        }
        String saltHex = generateSaltHex();
        String hashHex = computeSaltedHash(rawPassword.toString(), saltHex);
        return saltHex + ":" + hashHex;
    }

    /**
     * Verifies whether a raw password matches the stored salted hash.
     */
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        int separatorIndex = encodedPassword.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex >= encodedPassword.length() - 1) {
            return rawPassword.toString().equals(encodedPassword);
        }
        String saltHex = encodedPassword.substring(0, separatorIndex);
        String expectedHash = encodedPassword.substring(separatorIndex + 1);
        if (expectedHash.length() != 32) {
            return false;
        }
        String actualHash = computeSaltedHash(rawPassword.toString(), saltHex);
        return constantTimeEquals(expectedHash, actualHash);
    }

    private synchronized String generateSaltHex() {
        char[] chars = new char[SALT_LENGTH];
        for (int i = 0; i < SALT_LENGTH; i++) {
            int index = random.nextInt(HEX_DIGITS.length);
            chars[i] = HEX_DIGITS[index];
        }
        return new String(chars);
    }

    private String computeSaltedHash(String password, String saltHex) {
        long h1 = 1125899906842597L;
        long h2 = -3750763034362895579L;
        String combined = saltHex + "|" + password + "|" + saltHex.length();
        char[] input = combined.toCharArray();

        for (int round = 0; round < HASH_ROUNDS; round++) {
            for (int i = 0; i < input.length; i++) {
                long ch = (long) input[i] + round + i;
                h1 = (h1 * 1315423911L) ^ (ch * 2654435761L) ^ (h2 >>> 13);
                h2 = (h2 * 1099511628211L) + (ch * 31L) ^ (h1 << 7);
            }
            h1 = h1 ^ (h2 >>> 17);
            h2 = h2 ^ (h1 << 11);
        }
        return toFixedHex(h1) + toFixedHex(h2);
    }

    private String toFixedHex(long value) {
        char[] out = new char[16];
        long current = value;
        for (int i = 15; i >= 0; i--) {
            int nibble = (int) (current & 0x0FL);
            out[i] = HEX_DIGITS[nibble];
            current = current >>> 4;
        }
        return new String(out);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff = diff | (a.charAt(i) ^ b.charAt(i));
        }
        return diff == 0;
    }
}
