package com.acabaaqui.services;

import com.acabaaqui.model.AlgoritmoCredential;
import com.acabaaqui.model.UserCredential;
import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@ApplicationScoped
public class PasswordHasher {

    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final AlgoritmoCredential ALGORITHM = AlgoritmoCredential.pbkdf2;

    private final SecureRandom secureRandom = new SecureRandom();

    public void definirSenha(UserCredential credential, String password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);

        credential.setAlgoritmo(ALGORITHM);
        credential.setSalt(Base64.getEncoder().encodeToString(salt));
        credential.setSenhaHash(ITERATIONS + "$" + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS)));
    }

    public boolean verificarSenha(String password, UserCredential credential) {
        if (!ALGORITHM.equals(credential.getAlgoritmo()) || credential.getSenhaHash() == null
                || credential.getSalt() == null) {
            return false;
        }

        try {
            String[] hashParts = credential.getSenhaHash().split("\\$", 2);
            if (hashParts.length != 2) {
                return false;
            }

            int iterations = Integer.parseInt(hashParts[0]);
            if (iterations < 100_000 || iterations > 1_000_000) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(credential.getSalt());
            byte[] expectedHash = Base64.getDecoder().decode(hashParts[1]);
            byte[] actualHash = derive(password, salt, iterations);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível processar a senha", exception);
        } finally {
            spec.clearPassword();
        }
    }
}