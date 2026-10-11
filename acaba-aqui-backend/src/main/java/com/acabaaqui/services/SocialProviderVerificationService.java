package com.acabaaqui.services;

import com.acabaaqui.model.SocialAuthProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SocialProviderVerificationService {

    private static final String FACEBOOK_GRAPH_VERSION = "v25.0";

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "app.social-auth.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "app.social-auth.google-client-ids", defaultValue = "")
    String googleClientIds;

    @ConfigProperty(name = "app.social-auth.facebook-app-id", defaultValue = "")
    String facebookAppId;

    @ConfigProperty(name = "app.social-auth.facebook-app-secret", defaultValue = "")
    String facebookAppSecret;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public VerifiedIdentity verify(String providerValue, String credential) {
        if (!enabled) {
            throw new WebApplicationException("Login social desabilitado neste ambiente.",
                    Response.Status.NOT_FOUND);
        }
        if (credential == null || credential.isBlank() || credential.length() > 8192) {
            throw unauthorized();
        }

        SocialAuthProvider provider;
        try {
            provider = SocialAuthProvider.valueOf(providerValue.trim().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Provedor social inválido.");
        }

        return switch (provider) {
            case google -> verifyGoogle(credential);
            case facebook -> verifyFacebook(credential);
        };
    }

    private VerifiedIdentity verifyGoogle(String idToken) {
        Set<String> allowedAudiences = Arrays.stream(googleClientIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
        if (allowedAudiences.isEmpty()) {
            throw configurationError("GOOGLE_CLIENT_IDS não está configurado no backend.");
        }

        JsonNode claims = getJson("https://oauth2.googleapis.com/tokeninfo?id_token=" + encode(idToken));
        String issuer = text(claims, "iss");
        String audience = text(claims, "aud");
        long expiresAt = number(claims, "exp");
        String subject = text(claims, "sub");
        String email = text(claims, "email");
        String name = text(claims, "name");

        if (!(issuer.equals("accounts.google.com") || issuer.equals("https://accounts.google.com"))
                || !allowedAudiences.contains(audience)
                || expiresAt <= Instant.now().getEpochSecond()
                || subject.isBlank()
                || email.isBlank()
                || !isTrue(claims.get("email_verified"))) {
            throw unauthorized();
        }

        return new VerifiedIdentity(SocialAuthProvider.google, subject, name, email);
    }

    private VerifiedIdentity verifyFacebook(String accessToken) {
        if (facebookAppId.isBlank() || facebookAppSecret.isBlank()) {
            throw configurationError("FACEBOOK_APP_ID e FACEBOOK_APP_SECRET não estão configurados no backend.");
        }

        String graph = "https://graph.facebook.com/" + FACEBOOK_GRAPH_VERSION + "/";
        JsonNode debug = getJson(graph + "debug_token?input_token=" + encode(accessToken)
                + "&access_token=" + encode(facebookAppId + "|" + facebookAppSecret));
        JsonNode tokenData = debug.path("data");
        String subject = text(tokenData, "user_id");
        long expiresAt = number(tokenData, "expires_at");
        if (!isTrue(tokenData.get("is_valid"))
                || !facebookAppId.equals(text(tokenData, "app_id"))
                || subject.isBlank()
                || (expiresAt > 0 && expiresAt <= Instant.now().getEpochSecond())) {
            throw unauthorized();
        }

        JsonNode profile = getJson(graph + "me?fields=id,name,email&access_token=" + encode(accessToken));
        String email = text(profile, "email");
        if (!subject.equals(text(profile, "id")) || email.isBlank()) {
            throw new WebApplicationException(
                    "O Facebook não forneceu um e-mail. Use outro método de cadastro ou uma conta Google.",
                    Response.Status.BAD_REQUEST);
        }
        return new VerifiedIdentity(SocialAuthProvider.facebook, subject, text(profile, "name"), email);
    }

    private JsonNode getJson(String address) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(address))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 400 || response.statusCode() == 401) {
                throw unauthorized();
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerUnavailable();
            }
            JsonNode body = objectMapper.readTree(response.body());
            if (body == null || !body.isObject()) {
                throw providerUnavailable();
            }
            return body;
        } catch (IOException exception) {
            throw providerUnavailable();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw providerUnavailable();
        }
    }

    private static String text(JsonNode object, String field) {
        JsonNode value = object.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private static long number(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            return 0;
        }
        try {
            return value.isNumber() ? value.longValue() : Long.parseLong(value.asText());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static boolean isTrue(JsonNode value) {
        return value != null && (value.isBoolean() ? value.booleanValue()
                : Boolean.parseBoolean(value.asText()));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static WebApplicationException unauthorized() {
        return new WebApplicationException("Credencial social inválida ou expirada.",
                Response.Status.UNAUTHORIZED);
    }

    private static WebApplicationException configurationError(String message) {
        return new WebApplicationException(message, Response.Status.SERVICE_UNAVAILABLE);
    }

    private static WebApplicationException providerUnavailable() {
        return new WebApplicationException("Não foi possível validar a conta no provedor. Tente novamente.",
                Response.Status.BAD_GATEWAY);
    }

    public record VerifiedIdentity(SocialAuthProvider provider, String subject, String name, String email) { }
}
