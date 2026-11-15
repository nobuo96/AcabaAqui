package com.acabaaqui.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class MercadoPagoOrdersHttpGateway implements MercadoPagoOrderGateway {

    private static final Logger LOG = Logger.getLogger(MercadoPagoOrdersHttpGateway.class);
    private static final URI CREATE_ORDER_URI = URI.create("https://api.mercadopago.com/v1/orders");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final List<String> MERCADO_PAGO_DOMAINS = List.of(
            "mercadopago.com",
            "mercadopago.com.br",
            "mercadopago.com.ar",
            "mercadopago.com.mx",
            "mercadopago.cl",
            "mercadopago.com.co",
            "mercadopago.com.pe",
            "mercadopago.com.uy");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Inject
    public MercadoPagoOrdersHttpGateway(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    MercadoPagoOrdersHttpGateway(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public CheckoutOrder criarPedido(
            String accessToken,
            UUID idempotencyKey,
            String description,
            BigDecimal amount) {
        Map<String, Object> item = Map.of(
                "title", description,
                "quantity", 1,
                "unit_price", amount.toPlainString());
        Map<String, Object> body = Map.of(
                "type", "online",
                "processing_mode", "manual",
                "capture_mode", "automatic",
                "total_amount", amount.toPlainString(),
                "external_reference", idempotencyKey.toString(),
                "description", description,
                "items", List.of(item));

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            LOG.error("Could not serialize Mercado Pago order request.", exception);
            throw new WebApplicationException(
                    "Não foi possível preparar o pedido do Mercado Pago.",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        HttpRequest request = HttpRequest.newBuilder(CREATE_ORDER_URI)
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Idempotency-Key", idempotencyKey.toString())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOG.warn("Mercado Pago order request was interrupted.");
            throw new WebApplicationException(
                    "A comunicação com o Mercado Pago foi interrompida.",
                    Response.Status.BAD_GATEWAY);
        } catch (IOException exception) {
            LOG.error("Could not communicate with Mercado Pago Orders API.", exception);
            throw new WebApplicationException(
                    "Não foi possível comunicar com o Mercado Pago.",
                    Response.Status.BAD_GATEWAY);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOG.warnf("Mercado Pago order creation failed with HTTP status %d.", response.statusCode());
            throw new WebApplicationException(
                    "O Mercado Pago não aceitou a criação do pedido.",
                    Response.Status.BAD_GATEWAY);
        }

        try {
            JsonNode order = objectMapper.readTree(response.body());
            String orderId = requiredText(order, "id");
            String checkoutUrl = requiredText(order, "checkout_url");
            validateCheckoutUrl(checkoutUrl);
            return new CheckoutOrder(orderId, checkoutUrl);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            LOG.error("Mercado Pago returned an invalid order response.", exception);
            throw new WebApplicationException(
                    "O Mercado Pago retornou uma resposta de pedido inválida.",
                    Response.Status.BAD_GATEWAY);
        }
    }

    private static String requiredText(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing response field: " + field);
        }
        return value.asText();
    }

    private static void validateCheckoutUrl(String checkoutUrl) {
        URI uri = URI.create(checkoutUrl);
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || !isMercadoPagoHost(host)) {
            throw new IllegalArgumentException("Invalid Mercado Pago checkout URL.");
        }
    }

    private static boolean isMercadoPagoHost(String host) {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return MERCADO_PAGO_DOMAINS.stream()
                .anyMatch(domain -> normalizedHost.equals(domain) || normalizedHost.endsWith("." + domain));
    }
}
