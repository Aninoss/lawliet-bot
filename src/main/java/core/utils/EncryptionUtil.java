package core.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionUtil {

    public static final String SHA_CRYPT = "SHA-256";
    public static final String AES_ALGORITHM = "AES";
    public static final String AES_ALGORITHM_GCM = "AES/GCM/NoPadding";

    public static final Integer IV_LENGTH_ENCRYPT = 12;
    public static final Integer TAG_LENGTH_ENCRYPT = 16;

    public static final String ENCRYPTION_PASSPHRASE = System.getenv("ENCRYPTION_PASSPHRASE");

    public static String encrypt(String plainText) throws Exception {
        // Generate a random IV
        byte[] iv = new byte[IV_LENGTH_ENCRYPT];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);

        // Generate the AES key from the local passphrase
        SecretKeySpec aesKey = generateAesKeyFromPassphrase();

        // Initialize cipher in AES-GCM mode
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM_GCM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_ENCRYPT * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

        // Encrypt the plaintext
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Combine IV and encrypted text and encode them as Base64
        byte[] combinedIvAndCipherText = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combinedIvAndCipherText, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combinedIvAndCipherText, iv.length, encryptedBytes.length);

        return Base64.getEncoder().encodeToString(combinedIvAndCipherText);
    }

    public static String decrypt(String cipherText) throws Exception {
        byte[] decodedCipherText = Base64.getDecoder().decode(cipherText);

        // Generate the AES key from the local passphrase
        SecretKeySpec aesKey = generateAesKeyFromPassphrase();

        // Extract IV and encrypted text
        byte[] iv = new byte[IV_LENGTH_ENCRYPT];
        System.arraycopy(decodedCipherText, 0, iv, 0, iv.length);
        byte[] encryptedText = new byte[decodedCipherText.length - IV_LENGTH_ENCRYPT];
        System.arraycopy(decodedCipherText, IV_LENGTH_ENCRYPT, encryptedText, 0, encryptedText.length);

        // Initialize cipher in AES-GCM mode
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_ENCRYPT * 8, iv);
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM_GCM);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

        // Decrypt the ciphertext
        byte[] decryptedBytes = cipher.doFinal(encryptedText);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private static SecretKeySpec generateAesKeyFromPassphrase() throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance(SHA_CRYPT);
        byte[] keyBytes = sha256.digest(ENCRYPTION_PASSPHRASE.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }

}
