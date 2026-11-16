package com.acabaaqui.services;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercadoPagoWebhookSignatureVerifierTest {

    private static final String SECRET = "webhook-test-secret";

    @Test
    void acceptsValidSignatureAndNormalizesOrderIdCase() throws Exception {
        String signature = signature("ord-123", "request-456", "1710000000");
        MercadoPagoWebhookSignatureVerifier verifier =
                new MercadoPagoWebhookSignatureVerifier(SECRET);

        assertTrue(verifier.validar(signature, "request-456", "ORD-123"));
    }

    @Test
    void rejectsTamperedPayloadOrMissingSignatureParts() throws Exception {
        String signature = signature("ord-123", "request-456", "1710000000");
        MercadoPagoWebhookSignatureVerifier verifier =
                new MercadoPagoWebhookSignatureVerifier(SECRET);

        assertFalse(verifier.validar(signature, "request-456", "ord-999"));
        assertFalse(verifier.validar("ts=1710000000", "request-456", "ord-123"));
        assertFalse(verifier.validar(signature, "", "ord-123"));
    }

    @Test
    void refusesToValidateWithoutConfiguredSecret() throws Exception {
        String signature = signature("ord-123", "request-456", "1710000000");
        MercadoPagoWebhookSignatureVerifier verifier =
                new MercadoPagoWebhookSignatureVerifier("");

        assertFalse(verifier.validar(signature, "request-456", "ord-123"));
    }

    private static String signature(String orderId, String requestId, String timestamp) throws Exception {
        String manifest = "id:" + orderId.toLowerCase(java.util.Locale.ROOT)
                + ";request-id:" + requestId
                + ";ts:" + timestamp
                + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hash = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
        return "ts=" + timestamp + ",v1=" + hash;
    }
}
