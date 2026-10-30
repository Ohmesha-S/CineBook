package com.movieticket.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordEncoderTest {

    private final PasswordEncoder encoder = new PasswordEncoder();

    @Test
    @DisplayName("Should encode password and verify match with salted SHA-256")
    void testEncodeAndMatch() {
        String raw = "SecurePass@123";
        String encoded = encoder.encode(raw);

        assertNotNull(encoded);
        assertNotEquals(raw, encoded);
        assertTrue(encoded.contains(":"));

        assertTrue(encoder.matches(raw, encoded));
        assertFalse(encoder.matches("WrongPass", encoded));
    }

    @Test
    @DisplayName("Should generate different salt hashes for identical raw passwords")
    void testSaltRandomness() {
        String raw = "SamePassword";
        String hash1 = encoder.encode(raw);
        String hash2 = encoder.encode(raw);

        assertNotEquals(hash1, hash2);
        assertTrue(encoder.matches(raw, hash1));
        assertTrue(encoder.matches(raw, hash2));
    }
}
