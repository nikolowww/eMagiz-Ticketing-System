package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * checks the argon2 password helpers actually round trip the way we expect
 * these are a touch slower than the other unit tests becuase hashing is meant to be heavy
 * still no database needed here
 */
class PasswordTest {

    /**
     * hash a password then verify the same password against that hash
     * if this ever goes red nobody can log in
     */
    @Test
    void rightPasswordVerifies() {
        String hash = Main.hashPassword("support123");
        assertTrue(Main.verifyPassword("support123", hash));
    }

    /**
     * a wrong password must not verify otherwise the whole login is pointless
     */
    @Test
    void wrongPasswordDoesNotVerify() {
        String hash = Main.hashPassword("support123");
        assertFalse(Main.verifyPassword("hunter2", hash));
    }

    /**
     * argon2 salts every hash so the same password hashed twice should look diffrent
     * just a sanity check that we arent doing something silly and deterministic
     */
    @Test
    void samePasswordGivesDifferentHashes() {
        String a = Main.hashPassword("repeat me");
        String b = Main.hashPassword("repeat me");
        assertNotEquals(a, b);
        // both versions still have to verify even though the strings differ
        assertTrue(Main.verifyPassword("repeat me", a));
        assertTrue(Main.verifyPassword("repeat me", b));
    }
}
