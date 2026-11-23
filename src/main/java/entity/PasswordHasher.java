package entity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hashes account passwords and checks a typed password against a stored one.
 *
 * A stored credential looks like {@code pbkdf2$120000$<salt>$<hash>}, so the
 * iteration count and the per-user salt travel with the hash: changing the
 * defaults later still verifies older credentials. PBKDF2-HMAC-SHA256 ships
 * with the JDK, so hashing adds no dependency.
 *
 * A value that is not in that format is treated as a legacy plaintext password
 * and compared directly. That keeps accounts written before hashing existed
 * usable, and it is the only place plaintext is ever accepted.
 *
 * Comparisons use {@link MessageDigest#isEqual}, which does not stop at the
 * first differing byte, so a caller cannot learn how much of a hash matched.
 */
public final class PasswordHasher {

    /** Prefix that marks a hashed credential. */
    private static final String PREFIX = "pbkdf2";

    /** Key-derivation algorithm from the JDK. */
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /** Work factor used for new hashes. */
    private static final int ITERATIONS = 120_000;

    /** Length of the derived key in bits. */
    private static final int KEY_BITS = 256;

    /** Length of the random salt in bytes. */
    private static final int SALT_BYTES = 16;

    private static final String SEPARATOR = "$";
    private static final int PART_COUNT = 4;

    /** Utility class: not instantiable. */
    private PasswordHasher() {
    }

    /**
     * Hashes a password with a fresh random salt.
     *
     * @param password the plaintext password chosen by the user
     * @return the encoded credential, including the salt and iteration count
     */
    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("A password is required.");
        }
        final byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        final byte[] derived = derive(password, salt, ITERATIONS);
        return PREFIX + SEPARATOR + ITERATIONS + SEPARATOR
                + Base64.getEncoder().encodeToString(salt) + SEPARATOR
                + Base64.getEncoder().encodeToString(derived);
    }

    /**
     * Says whether a typed password matches a stored credential.
     *
     * @param typedPassword     what the user just typed
     * @param storedCredential  what the store holds for that account
     * @return true only when the password is correct
     */
    public static boolean matches(String typedPassword, String storedCredential) {
        if (typedPassword == null || storedCredential == null || storedCredential.isEmpty()) {
            return false;
        }
        final String[] parts = storedCredential.split("\\" + SEPARATOR);
        if (parts.length != PART_COUNT || !PREFIX.equals(parts[0])) {
            return matchesPlaintext(typedPassword, storedCredential);
        }

        final int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        }
        catch (NumberFormatException ex) {
            return false;
        }
        if (iterations <= 0) {
            return false;
        }

        try {
            final byte[] salt = Base64.getDecoder().decode(parts[2]);
            final byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, derive(typedPassword, salt, iterations));
        }
        catch (IllegalArgumentException ex) {
            // A malformed hash cannot match anything.
            return false;
        }
    }

    /**
     * Says whether a stored credential is already hashed.
     *
     * @param storedCredential the value held by the store
     * @return true when the value carries a salt and iteration count
     */
    public static boolean isHashed(String storedCredential) {
        return storedCredential != null && storedCredential.startsWith(PREFIX + SEPARATOR);
    }

    /** Compares a legacy plaintext credential without early exit. */
    private static boolean matchesPlaintext(String typedPassword, String storedCredential) {
        return MessageDigest.isEqual(
                typedPassword.getBytes(StandardCharsets.UTF_8),
                storedCredential.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Runs the key derivation.
     *
     * @param password   the plaintext password
     * @param salt       the salt to mix in
     * @param iterations the work factor
     * @return the derived key
     */
    private static byte[] derive(String password, byte[] salt, int iterations) {
        final PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Could not hash a password with " + ALGORITHM, ex);
        }
        finally {
            spec.clearPassword();
        }
    }
}
