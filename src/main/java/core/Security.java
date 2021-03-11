package core;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Random;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Security {

    public static String getHashForString(String salt, String string) throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] saltByte = new byte[16];
        new Random(salt.hashCode()).nextBytes(saltByte);
        KeySpec spec = new PBEKeySpec(string.toCharArray(), saltByte, 65536, 128);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = f.generateSecret(spec).getEncoded();
        Base64.Encoder enc = Base64.getEncoder();
        return enc.encodeToString(hash);
    }

}
