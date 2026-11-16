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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
        return criarPedido(
                accessToken,
                idempotencyKey,
                idempotencyKey.toString(),
                description,
                amount,
                null);
    }

    @Override
    public CheckoutOrder criarPedido(
            String accessToken,
            UUID idempotencyKey,
            String externalReference,
            String description,
            BigDecimal amount,
            ReturnUrls returnUrls) {
        Map<String, Object> item = Map.of(
                "title", description,
                "quantity", 1,
                "unit_price", amount.toPlainString());
        Map<String, Object> body = new java.util.HashMap<>(Map.of(
                "type", "online",
                "processing_mode", "manual",
                "capture_mode", returnUrls == null ? "automatic" : "automatic_async",
                "total_amount", amount.toPlainString(),
                "external_reference", externalReference,
                "description", description,
                "items", List.of(item)));
        if (returnUrls != null) {
            body.put("config", Map.of(
                    "online", Map.of(
                            "success_url", returnUrls.successUrl(),
                            "failure_url", returnUrls.failureUrl(),
                            "pending_url", returnUrls.pendingUrl(),
                            "auto_return", "approved")));
        }

        JsonNode order = sendJson(
                HttpRequest.newBuilder(CREATE_ORDER_URI)
                        .header("X-Idempotency-Key", idempotencyKey.toString())
                        .POST(HttpRequest.BodyPublishers.ofString(serialize(body))),
                accessToken,
                "order creation");
        try {
            String orderId = requiredText(order, "id");
            String checkoutUrl = requiredText(order, "checkout_url");
            validateCheckoutUrl(checkoutUrl);
            return new CheckoutOrder(orderId, checkoutUrl);
        } catch (IllegalArgumentException exception) {
            LOG.error("Mercado Pago returned an invalid order response.", exception);
            throw gatewayError("O Mercado Pago retornou uma resposta de pedido inválida.");
        }
    }

    @Override
    public OrderSnapshot consultarPedido(String accessToken, String orderId) {
        return snapshot(sendJson(
                HttpRequest.newBuilder(orderUri(orderId)).GET(),
                accessToken,
                "order query"));
    }

    @Override
    public OrderSnapshot cancelarPedido(String accessToken, String orderId, UUID idempotencyKey) {
        try {
            sendJson(
                    HttpRequest.newBuilder(orderUri(orderId, "cancel"))
                            .header("X-Idempotency-Key", idempotencyKey.toString())
                            .POST(HttpRequest.BodyPublishers.noBody()),
                    accessToken,
                    "order cancellation");
            return consultarPedido(accessToken, orderId);
        } catch (WebApplicationException exception) {
            if (exception.getResponse().getStatus() != Response.Status.CONFLICT.getStatusCode()) {
                throw exception;
            }
            return consultarPedido(accessToken, orderId);
        }
    }

    @Override
    public OrderSnapshot reembolsarPedido(String accessToken, String orderId, UUID idempotencyKey) {
        try {
            sendJson(
                    HttpRequest.newBuilder(orderUri(orderId, "refund"))
                            .header("X-Idempotency-Key", idempotencyKey.toString())
                            .POST(HttpRequest.BodyPublishers.noBody()),
                    accessToken,
                    "order refund");
            return consultarPedido(accessToken, orderId);
        } catch (WebApplicationException exception) {
            if (exception.getResponse().getStatus() != Response.Status.CONFLICT.getStatusCode()) {
                throw exception;
            }
            return consultarPedido(accessToken, orderId);
        }
    }

    private JsonNode sendJson(HttpRequest.Builder requestBuilder, String accessToken, String operation) {
        HttpRequest request = requestBuilder
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOG.warnf("Mercado Pago %s request was interrupted.", operation);
            throw new WebApplicationException(
                    "A comunicação com o Mercado Pago foi interrompida.",
                    Response.Status.BAD_GATEWAY);
        } catch (IOException exception) {
            LOG.errorf(exception, "Could not communicate with Mercado Pago during %s.", operation);
            throw gatewayError("Não foi possível comunicar com o Mercado Pago.");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOG.warnf("Mercado Pago %s failed with HTTP status %d.", operation, response.statusCode());
            if (response.statusCode() == Response.Status.CONFLICT.getStatusCode()) {
                throw new WebApplicationException(
                        "A Order do Mercado Pago já foi alterada ou não aceita esta operação.",
                        Response.Status.CONFLICT);
            }
            throw gatewayError("O Mercado Pago não aceitou a solicitação.");
        }
        try {
            JsonNode result = objectMapper.readTree(response.body());
            if (result == null || !result.isObject()) {
                throw new IllegalArgumentException("Expected a JSON object.");
            }
            return result;
        } catch (JsonProcessingException exception) {
            LOG.errorf(exception, "Mercado Pago returned invalid JSON during %s.", operation);
            throw gatewayError("O Mercado Pago retornou uma resposta inválida.");
        } catch (IllegalArgumentException exception) {
            LOG.errorf(exception, "Mercado Pago returned an invalid response during %s.", operation);
            throw gatewayError("O Mercado Pago retornou uma resposta inválida.");
        }
    }

    private String serialize(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            LOG.error("Could not serialize Mercado Pago order request.", exception);
            throw new WebApplicationException(
                    "Não foi possível preparar o pedido do Mercado Pago.",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private OrderSnapshot snapshot(JsonNode order) {
        try {
            if (order == null || !order.isObject()) {
                throw new IllegalArgumentException("Expected a JSON object.");
            }
            String orderId = requiredText(order, "id");
            String externalReference = requiredText(order, "external_reference");
            String status = requiredText(order, "status");
            String statusDetail = order.hasNonNull("status_detail")
                    ? order.get("status_detail").asText()
                    : "";
            BigDecimal totalPaid = order.hasNonNull("total_paid_amount")
                    ? new BigDecimal(order.get("total_paid_amount").asText())
                    : BigDecimal.ZERO;
            return new OrderSnapshot(orderId, externalReference, status, statusDetail, totalPaid);
        } catch (IllegalArgumentException exception) {
            LOG.error("Mercado Pago returned an invalid order status response.", exception);
            throw gatewayError("O Mercado Pago retornou uma resposta de pedido inválida.");
        }
    }

    private static URI orderUri(String orderId, String... action) {
        String encodedId = URLEncoder.encode(orderId, StandardCharsets.UTF_8).replace("+", "%20");
        String suffix = action.length == 0 ? "" : "/" + action[0];
        return URI.create("https://api.mercadopago.com/v1/orders/" + encodedId + suffix);
    }

    private static WebApplicationException gatewayError(String message) {
        return new WebApplicationException(message, Response.Status.BAD_GATEWAY);
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
