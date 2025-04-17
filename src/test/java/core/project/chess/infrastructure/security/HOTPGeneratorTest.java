package core.project.chess.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class HOTPGeneratorTest {

    @Test
    void testGenerateSecretKey() {
        String key = HOTPGenerator.generateSecretKey();
        assertNotNull(key);
        assertFalse(key.isBlank());
        byte[] decoded = Base64.getDecoder().decode(key);
        assertEquals(20, decoded.length);
    }

    @Test
    void testGenerateHOTP_Default() {
        HOTPGenerator generator = new HOTPGenerator();
        String key = HOTPGenerator.generateSecretKey();
        String hotp = generator.generateHOTP(key, 1L);
        assertNotNull(hotp);
        assertEquals(6, hotp.length());
        assertTrue(hotp.matches("\\d{6}"));
    }

    @Test
    void testGenerateHOTP_DifferentLengths() {
        String key = HOTPGenerator.generateSecretKey();

        HOTPGenerator gen6 = new HOTPGenerator(HOTPGenerator.DEFAULT_ALGORITHM, 6);
        HOTPGenerator gen7 = new HOTPGenerator(HOTPGenerator.DEFAULT_ALGORITHM, 7);
        HOTPGenerator gen8 = new HOTPGenerator(HOTPGenerator.DEFAULT_ALGORITHM, 8);

        assertEquals(6, gen6.generateHOTP(key, 1L).length());
        assertEquals(7, gen7.generateHOTP(key, 1L).length());
        assertEquals(8, gen8.generateHOTP(key, 1L).length());
    }

    @Test
    void testGenerateHOTP_ConsistentOutput() {
        HOTPGenerator generator = new HOTPGenerator();
        String key = HOTPGenerator.generateSecretKey();
        long counter = 123456L;

        String first = generator.generateHOTP(key, counter);
        String second = generator.generateHOTP(key, counter);

        assertEquals(first, second);
    }

    @Test
    void testInvalidPasswordLength() {
        assertThrows(IllegalArgumentException.class, () -> new HOTPGenerator(HOTPGenerator.DEFAULT_ALGORITHM, 5));
    }

    @Test
    void testInvalidAlgorithm() {
        assertThrows(IllegalArgumentException.class, () -> new HOTPGenerator("InvalidAlgo"));
    }

    @Test
    void testInvalidBase64Key() {
        HOTPGenerator generator = new HOTPGenerator();
        assertThrows(IllegalArgumentException.class, () -> generator.generateHOTP("this_is_not_base64", 10L));
    }

    @Test
    void testLargeCounter() {
        HOTPGenerator generator = new HOTPGenerator();
        String key = HOTPGenerator.generateSecretKey();
        String hotp = generator.generateHOTP(key, Long.MAX_VALUE);
        assertNotNull(hotp);
        assertEquals(6, hotp.length());
    }

    @Test
    void testHOTPChangesWithCounter() {
        HOTPGenerator generator = new HOTPGenerator();
        String key = HOTPGenerator.generateSecretKey();

        String code1 = generator.generateHOTP(key, 1L);
        String code2 = generator.generateHOTP(key, 2L);

        assertNotEquals(code1, code2);
    }
}