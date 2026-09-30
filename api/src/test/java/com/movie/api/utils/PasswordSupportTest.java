package com.movie.api.utils;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

class PasswordSupportTest {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Test
    void omittedBlankAndLegacyResubmittedHashPreserveThePassword() {
        String existing = encoder.encode("original-test-password");
        assertNull(PasswordSupport.forUpdate(null, existing, encoder));
        assertNull(PasswordSupport.forUpdate("", existing, encoder));
        assertNull(PasswordSupport.forUpdate("  ", existing, encoder));
        assertNull(PasswordSupport.forUpdate(existing, existing, encoder));
    }

    @Test
    void changedPasswordIsEncodedOnce() {
        String encoded = PasswordSupport.forUpdate("new-test-password", "previous-hash", encoder);
        assertNotEquals("new-test-password", encoded);
        assertTrue(encoder.matches("new-test-password", encoded));
    }

    @Test
    void rejectsBlankCreationPasswords() {
        assertThrows(IllegalArgumentException.class, () -> PasswordSupport.forCreate(null, encoder));
        assertThrows(IllegalArgumentException.class, () -> PasswordSupport.forCreate(" ", encoder));
    }
}
