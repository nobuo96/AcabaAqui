package com.acabaaqui.services;

import com.mercadopago.client.user.UserClient;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@ApplicationScoped
public class MercadoPagoPaymentService {

    private static final Logger LOG = Logger.getLogger(MercadoPagoPaymentService.class);

    private final String accessToken;
    private final UserClient userClient;
    private final MercadoPagoOrderGateway orderGateway;
    private final boolean sandboxOrderTestEnabled;

    @Inject
    public MercadoPagoPaymentService(
            @ConfigProperty(name = "mercadopago.access-token", defaultValue = "") String accessToken,
            @ConfigProperty(name = "mercadopago.orders.sandbox-test-enabled", defaultValue = "false")
                    boolean sandboxOrderTestEnabled,
            MercadoPagoOrderGateway orderGateway) {
        this(accessToken, new UserClient(), orderGateway, sandboxOrderTestEnabled);
    }

    MercadoPagoPaymentService(String accessToken, UserClient userClient) {
        this(accessToken, userClient, null, false);
    }

    MercadoPagoPaymentService(
            String accessToken,
            UserClient userClient,
            MercadoPagoOrderGateway orderGateway) {
        this(accessToken, userClient, orderGateway, false);
    }

    MercadoPagoPaymentService(
            String accessToken,
            UserClient userClient,
            MercadoPagoOrderGateway orderGateway,
            boolean sandboxOrderTestEnabled) {
        this.accessToken = accessToken;
        this.userClient = userClient;
        this.orderGateway = orderGateway;
        this.sandboxOrderTestEnabled = sandboxOrderTestEnabled;
    }

    public ConnectionCheck verificarConexao() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new WebApplicationException(
                    "Configure MERCADOPAGO_ACCESS_TOKEN no ambiente do backend.",
                    Response.Status.SERVICE_UNAVAILABLE);
        }

        MPRequestOptions options = MPRequestOptions.builder()
                .accessToken(accessToken)
                .connectionTimeout(5000)
                .socketTimeout(10000)
                .build();

        try {
            User account = userClient.get(options);
            if (account == null) {
                throw new WebApplicationException(
                        "O Mercado Pago retornou uma conta inválida.",
                        Response.Status.BAD_GATEWAY);
            }
            return new ConnectionCheck(true, "Conexão com o Mercado Pago validada.");
        } catch (MPApiException exception) {
            LOG.warnf("Mercado Pago integration check failed with HTTP status %d.",
                    exception.getStatusCode());
            throw new WebApplicationException(
                    "O Mercado Pago rejeitou a credencial ou a solicitação.",
                    Response.Status.BAD_GATEWAY);
        } catch (MPException exception) {
            LOG.error("Não foi possível comunicar com o Mercado Pago.", exception);
            throw new WebApplicationException(
                    "Não foi possível comunicar com o Mercado Pago.",
                    exception,
                    Response.Status.BAD_GATEWAY);
        }
    }

    public MercadoPagoOrderGateway.CheckoutOrder criarPedidoSandbox(
            String description,
            BigDecimal amount,
            String idempotencyKey) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new WebApplicationException(
                    "Configure MERCADOPAGO_ACCESS_TOKEN no ambiente do backend.",
                    Response.Status.SERVICE_UNAVAILABLE);
        }
        if (!sandboxOrderTestEnabled) {
            throw new WebApplicationException(
                    "A criação de pedidos de teste está desabilitada. Configure "
                            + "MERCADOPAGO_ORDERS_SANDBOX_TEST_ENABLED=true somente em ambiente local.",
                    Response.Status.SERVICE_UNAVAILABLE);
        }
        if (description == null || description.isBlank() || description.trim().length() > 120) {
            throw new WebApplicationException(
                    "A descrição é obrigatória e deve ter até 120 caracteres.",
                    Response.Status.BAD_REQUEST);
        }
        if (amount == null || amount.signum() <= 0) {
            throw new WebApplicationException(
                    "O valor deve ser maior que zero.",
                    Response.Status.BAD_REQUEST);
        }

        BigDecimal normalizedAmount;
        try {
            normalizedAmount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new WebApplicationException(
                    "O valor deve ter no máximo duas casas decimais.",
                    Response.Status.BAD_REQUEST);
        }

        UUID key;
        try {
            key = UUID.fromString(idempotencyKey);
        } catch (RuntimeException exception) {
            throw new WebApplicationException(
                    "Informe um X-Idempotency-Key válido no formato UUID.",
                    Response.Status.BAD_REQUEST);
        }

        if (orderGateway == null) {
            LOG.error("Mercado Pago order gateway is not configured.");
            throw new WebApplicationException(
                    "A integração de pedidos do Mercado Pago não está configurada.",
                    Response.Status.SERVICE_UNAVAILABLE);
        }

        return orderGateway.criarPedido(
                accessToken,
                key,
                description.trim(),
                normalizedAmount);
    }

    public record ConnectionCheck(boolean connected, String message) { }
}
