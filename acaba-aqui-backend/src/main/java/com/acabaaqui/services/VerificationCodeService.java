package com.acabaaqui.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class VerificationCodeService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] secret;

    @Inject
    public VerificationCodeService(@ConfigProperty(name = "app.email-verification.hmac-secret") String encodedSecret) {
        try {
            this.secret = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("APP_EMAIL_VERIFICATION_SECRET deve ser Base64", exception);
        }
        if (secret.length < 32) {
            throw new IllegalStateException("APP_EMAIL_VERIFICATION_SECRET deve conter pelo menos 32 bytes");
        }
    }

    public String gerarCodigo() {
        return String.format(java.util.Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
    }

    public String calcularHmac(String email, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal((email + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível proteger o código de verificação", exception);
        }
    }

    public boolean verificar(String email, String code, String expectedHmac) {
        byte[] actual = calcularHmac(email, code).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = expectedHmac.getBytes(StandardCharsets.US_ASCII);
        return java.security.MessageDigest.isEqual(expected, actual);
    }
}