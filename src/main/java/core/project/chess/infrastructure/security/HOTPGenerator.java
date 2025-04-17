package core.project.chess.infrastructure.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * HOTPGenerator is a utility class that generates one-time passwords (OTPs) using the HOTP algorithm.
 * <p>
 * HOTP stands for "HMAC-based One-Time Password". It is a secure authentication method based on:
 * - A shared secret key between client and server
 * - A counter that increments with each new code
 * <p>
 * HOTP is widely used for two-factor authentication (2FA), including SMS codes, hardware tokens,
 * and mobile apps like Google Authenticator.
 * <p>
 * This class supports generating numeric HOTP codes (usually 6-8 digits) using HMAC algorithms
 * like HmacSHA1, HmacSHA256, or HmacSHA512.
 *
 * @author Hadzhyiev Hadzhy
 */
public class HOTPGenerator {
    private final Mac mac;
    private final int passwordLength;
    private final int modDivisor;
    public static final String DEFAULT_ALGORITHM = "HmacSHA256";
    public static final int DEFAULT_LENGTH = 6;

    /**
     * Creates a HOTPGenerator with default settings.
     * <p>
     * Uses HmacSHA256 as the hashing algorithm and produces 6-digit codes.
     */
    public HOTPGenerator() {
        try {
            this.mac = Mac.getInstance(DEFAULT_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Invalid algorithm: %s"
                    .formatted(e.getLocalizedMessage()));
        }
        this.passwordLength = DEFAULT_LENGTH;
        this.modDivisor = 1_000_000;
    }

    /**
     * Creates a HOTPGenerator with a custom HMAC algorithm (e.g., HmacSHA1, HmacSHA512).
     *
     * @param algorithm The name of the hashing algorithm to use (must be supported by Java's Mac class,
     * check <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#mac-algorithms">
     *                  Mac algorithms
     *       </a>).
     */
    public HOTPGenerator(String algorithm) {
        try {
            this.mac = Mac.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Invalid algorithm: %s"
                    .formatted(e.getLocalizedMessage()));
        }

        this.passwordLength = DEFAULT_LENGTH;
        this.modDivisor = 1_000_000;
    }

    /**
     * Creates a HOTPGenerator with custom algorithm and code length (6–8 digits).
     *
     * @param algorithm      Hashing algorithm (e.g., HmacSHA1).
     * @param passwordLength Desired code length (must be 6, 7, or 8).
     * @throws IllegalArgumentException if the length is not 6–8 digits.
     */
    public HOTPGenerator(String algorithm, int passwordLength) {
        try {
            this.mac = Mac.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Invalid algorithm: %s"
                    .formatted(e.getLocalizedMessage()));
        }
        switch (passwordLength) {
            case 6 -> this.modDivisor = 1_000_000;
            case 7 -> this.modDivisor = 10_000_000;
            case 8 -> this.modDivisor = 100_000_000;
            default -> throw new IllegalArgumentException("Invalid password size");
        }
        this.passwordLength = passwordLength;
    }

    /**
     * Generates a one-time password (HOTP) based on a secret key and a counter value.
     * <p>
     * HOTP (HMAC-based One-Time Password) is a secure method used to generate short numeric codes
     * that are valid only once.
     * <p>
     * Each generated code depends on a shared secret key and a moving counter value,
     * ensuring its predictable only to the user and the server.
     *
     * @param base64Key The secret key encoded in Base64 format.
     * @param counter A number that increases with every new code request. The server and user must stay in sync.
     * @return A numeric code (e.g., 6 digits) as a String, generated based on the key and counter.
     * @throws IllegalArgumentException if something goes wrong during the process (e.g., bad key format).
     */
    public String generateHOTP(String base64Key, long counter) {
        try {
            // Decode the Base64 key back into raw bytes
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            // Convert the counter number into an 8-byte array (standard requirement for HOTP)
            byte[] counterBytes = ByteBuffer.allocate(Long.BYTES).putLong(counter).array();

            // Prepare the key for use in the MAC (Message Authentication Code) algorithm
            SecretKeySpec signKey = new SecretKeySpec(keyBytes, "RAW");
            mac.init(signKey);

            // Compute the HMAC hash of the counter using the secret key
            byte[] hash = mac.doFinal(counterBytes);

            // Extract the one-time code from the hash
            long truncatedHash = getTruncatedHash(hash);

            // Format the number to be zero-padded to the desired length (e.g., "000123" for a 6-digit code)
            return String.format("%0" + passwordLength + "d", truncatedHash);
        } catch (Exception e) {
            throw new IllegalArgumentException("Can`t generate HOTP. %s".formatted(e.getLocalizedMessage()));
        }
    }

    /**
     * Extracts a short numeric value from a cryptographic hash (HMAC), following HOTP standard rules.
     * <p>
     * This process is called "dynamic truncation" and is defined in the official HOTP specification (RFC 4226).
     * It ensures the generated code is both secure and unpredictable.
     *
     * @param hash A 20-byte HMAC result, generated from the counter and secret key.
     * @return A positive number (e.g., 6 digits) extracted from the hash and ready to be formatted.
     */
    private long getTruncatedHash(byte[] hash) {
        // Step 1: Create a mask (0xF = 00001111 in binary) to extract the last 4 bits
        // This is used to calculate a safe offset within the hash array (0–15)
        int mask = ~(~0 << 4); // Equivalent to 0xF (00001111)
        byte lastByte = hash[hash.length - 1]; // Take the last byte of the HMAC result
        int offset = lastByte & mask; // Extract last 4 bits of that byte
        /* For example:
            lastByte:    11011011  => (219)
            mask:        00001111
            result:      00001011  => (11)
         */

        // Step 2: Extract 4 bytes starting at the calculated offset
        // These 4 bytes will be interpreted as an integer to generate the OTP
        byte[] truncatedHashInBytes = {
                hash[offset],
                hash[offset + 1],
                hash[offset + 2],
                hash[offset + 3]
        };

        // Step 3: Convert the 4 bytes into a single 32-bit integer
        // ByteBuffer helps interpret the byte array as a number
        ByteBuffer byteBuffer = ByteBuffer.wrap(truncatedHashInBytes);
        long truncatedHash = byteBuffer.getInt(); // Still may be negative

        // Step 4: Clear the most significant (sign) bit
        // Ensures the result is a non-negative number by applying a bitmask
        truncatedHash &= 0x7FFFFFFF; // Equivalent to keeping only the lowest 31 bits

        // Step 5: Reduce the number to a fixed-length decimal code
        // For example, if passwordLength = 6, result will be in range [000000, 999999]
        truncatedHash %= (long) Math.pow(10, passwordLength);

        // Return the final truncated and formatted HOTP value
        return truncatedHash;

    }

    /**
     * Generates a new secure random secret key encoded in Base64.
     * <p>
     * This key should be stored securely. Do not send to the client.
     * <p>
     * The key is 160 bits long (20 bytes), suitable for HOTP and TOTP algorithms.
     *
     * @return A Base64-encoded secure key string.
     */
    public static String generateSecretKey() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return Base64.getEncoder().encodeToString(buffer);
    }
}
