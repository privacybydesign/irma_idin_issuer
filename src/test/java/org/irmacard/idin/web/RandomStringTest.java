package org.irmacard.idin.web;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class RandomStringTest {

    /**
     * A tiny predictable Random for white-box checks.
     */
    static final class FixedRandom extends Random {
        private final int value;

        FixedRandom(final int value) {
            this.value = value;
        }

        @Override
        public int nextInt(final int bound) {
            return value % bound;
        }
    }

    @Test
    public void generatesStringOfRequestedLength_withOnlyProvidedSymbols() {
        final String symbols = "ABC";
        final RandomString rs = new RandomString(100, new Random(42), symbols);

        final String s = rs.nextString();

        assertEquals(100, s.length());
        for (final char c : s.toCharArray()) {
            assertTrue(symbols.indexOf(c) >= 0, "Unexpected char: " + c);
        }
    }

    @Test
    public void deterministicGivenSameSeedAndParameters() {
        final RandomString a = new RandomString(20, new Random(12345L), RandomString.alphanum);
        final RandomString b = new RandomString(20, new Random(12345L), RandomString.alphanum);

        assertEquals(a.nextString(), b.nextString());
        assertEquals(a.nextString(), b.nextString());
    }

    @Test
    public void usesProvidedRandomAndSymbols_exactly() {
        final RandomString rs = new RandomString(5, new FixedRandom(1), "ABC");
        assertEquals("BBBBB", rs.nextString());
    }

    @Test
    public void returnsNewStringObjectEachCall() {
        final RandomString rs = new RandomString(8, new Random(7), RandomString.alphanum);
        final String s1 = rs.nextString();
        final String s2 = rs.nextString();
        assertNotSame(s1, s2, "Should create a fresh String each call");
    }

    @Test
    public void constructorRejectsLengthLessThanOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new RandomString(0, new Random(), "AB"));
        assertThrows(IllegalArgumentException.class,
                () -> new RandomString(-1, new Random(), "AB"));
    }

    @Test
    public void constructorRejectsTooFewSymbols() {
        assertThrows(IllegalArgumentException.class,
                () -> new RandomString(5, new Random(), "A"));
    }

    @Test
    public void constructorRejectsNullRandom() {
        assertThrows(NullPointerException.class,
                () -> new RandomString(5, null, "AB"));
    }

    @Test
    public void constructorRejectsNullSymbols() {
        assertThrows(NullPointerException.class,
                () -> new RandomString(5, new Random(), null));
    }
}
