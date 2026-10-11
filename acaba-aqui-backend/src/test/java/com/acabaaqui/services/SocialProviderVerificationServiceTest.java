package com.acabaaqui.services;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SocialProviderVerificationServiceTest {

    @Test
    void rejectsSocialLoginWhenDisabled() {
        SocialProviderVerificationService service = new SocialProviderVerificationService();

        WebApplicationException exception = assertThrows(
                WebApplicationException.class, () -> service.verify("google", "test-token"));

        assertEquals(404, exception.getResponse().getStatus());
    }

    @Test
    void requiresGoogleClientIdsBeforeContactingProvider() {
        SocialProviderVerificationService service = new SocialProviderVerificationService();
        service.enabled = true;
        service.googleClientIds = "";

        WebApplicationException exception = assertThrows(
                WebApplicationException.class, () -> service.verify("google", "test-token"));

        assertEquals(503, exception.getResponse().getStatus());
    }

    @Test
    void requiresFacebookAppCredentialsBeforeContactingProvider() {
        SocialProviderVerificationService service = new SocialProviderVerificationService();
        service.enabled = true;
        service.facebookAppId = "";
        service.facebookAppSecret = "";

        WebApplicationException exception = assertThrows(
                WebApplicationException.class, () -> service.verify("facebook", "test-token"));

        assertEquals(503, exception.getResponse().getStatus());
    }

    @Test
    void rejectsUnknownProvider() {
        SocialProviderVerificationService service = new SocialProviderVerificationService();
        service.enabled = true;

        assertThrows(IllegalArgumentException.class, () -> service.verify("other", "test-token"));
    }
}
