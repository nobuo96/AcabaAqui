package com.acabaaqui.services;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class MercadoPagoWebhookSignatureVerifier {

    private final String secret;

    public MercadoPagoWebhookSignatureVerifier(
            @ConfigProperty(name = "mercadopago.webhook-secret", defaultValue = "") String secret) {
        this.secret = secret;
    }

    public boolean validar(String signature, String requestId, String dataId) {
        if (secret == null || secret.isBlank()
                || signature == null || requestId == null || requestId.isBlank()
                || dataId == null || dataId.isBlank()) {
            return false;
        }

        Map<String, String> signatureParts = parseSignature(signature);
        String timestamp = signatureParts.get("ts");
        String providedSignature = signatureParts.get("v1");
        if (timestamp == null || providedSignature == null) {
            return false;
        }

        String manifest = "id:" + dataId.toLowerCase(java.util.Locale.ROOT)
                + ";request-id:" + requestId
                + ";ts:" + timestamp
                + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = toHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)))
                    .getBytes(StandardCharsets.US_ASCII);
            byte[] actual = providedSignature.toLowerCase(java.util.Locale.ROOT)
                    .getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, actual);
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível validar a assinatura do webhook.", exception);
        }
    }

    public boolean configurado() {
        return secret != null && !secret.isBlank();
    }

    private static Map<String, String> parseSignature(String signature) {
        Map<String, String> parts = new HashMap<>();
        for (String part : signature.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2) {
                parts.put(pair[0].trim(), pair[1].trim());
            }
        }
        return parts;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >> 4) & 0xF, 16));
            result.append(Character.forDigit(value & 0xF, 16));
        }
        return result.toString();
    }
}
