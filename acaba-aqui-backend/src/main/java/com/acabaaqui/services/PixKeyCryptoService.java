package com.acabaaqui.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@ApplicationScoped
public class PixKeyCryptoService {

    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @ConfigProperty(name = "app.pix-key-encryption-secret", defaultValue = "")
    String encryptionSecret;

    public String encrypt(String value) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            SECURE_RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] payload = Arrays.copyOf(nonce, nonce.length + encrypted.length);
            System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new WebApplicationException(
                    "Não foi possível proteger a chave Pix.",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue);
            if (payload.length <= NONCE_LENGTH) {
                throw new GeneralSecurityException("Invalid encrypted Pix key.");
            }
            byte[] nonce = Arrays.copyOfRange(payload, 0, NONCE_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(payload, NONCE_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new WebApplicationException(
                    "Não foi possível descriptografar a chave Pix. Verifique APP_PIX_KEY_ENCRYPTION_SECRET.",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private SecretKey secretKey() {
        if (encryptionSecret == null || encryptionSecret.isBlank()) {
            throw new WebApplicationException(
                    "Configure APP_PIX_KEY_ENCRYPTION_SECRET antes de cadastrar ou visualizar chaves Pix.",
                    Response.Status.SERVICE_UNAVAILABLE);
        }
        try {
            byte[] key = Base64.getDecoder().decode(encryptionSecret);
            if (key.length != 32) {
                throw new IllegalArgumentException("Invalid AES key length.");
            }
            return new SecretKeySpec(key, "AES");
        } catch (IllegalArgumentException exception) {
            throw new WebApplicationException(
                    "APP_PIX_KEY_ENCRYPTION_SECRET deve ser uma chave Base64 de 32 bytes.",
                    Response.Status.SERVICE_UNAVAILABLE);
        }
    }
}
