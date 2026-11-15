package com.acabaaqui.services;

import com.mercadopago.client.user.UserClient;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.user.User;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercadoPagoPaymentServiceTest {

    @Test
    void verifiesTheConfiguredTokenWithoutCreatingACharge() {
        StubUserClient userClient = new StubUserClient(new User());
        MercadoPagoPaymentService service = new MercadoPagoPaymentService("test-token", userClient);

        MercadoPagoPaymentService.ConnectionCheck result = service.verificarConexao();

        assertTrue(result.connected());
        assertNotNull(userClient.options);
        assertEquals("test-token", userClient.options.getAccessToken());
    }

    @Test
    void requiresAnAccessTokenBeforeCallingTheSdk() {
        StubUserClient userClient = new StubUserClient(new User());
        MercadoPagoPaymentService service = new MercadoPagoPaymentService(" ", userClient);

        WebApplicationException exception = assertThrows(
                WebApplicationException.class, service::verificarConexao);

        assertEquals(503, exception.getResponse().getStatus());
        assertFalse(userClient.called);
    }

    @Test
    void reportsSdkCommunicationFailuresAsGatewayErrors() {
        MercadoPagoPaymentService service = new MercadoPagoPaymentService(
                "test-token",
                new FailingUserClient());

        WebApplicationException exception = assertThrows(
                WebApplicationException.class, service::verificarConexao);

        assertEquals(502, exception.getResponse().getStatus());
    }

    @Test
    void createsSandboxOrderWhenExplicitlyEnabledWithTheConfiguredToken() {
        StubOrderGateway orderGateway = new StubOrderGateway();
        MercadoPagoPaymentService service = new MercadoPagoPaymentService(
                "APP-sandbox-token",
                new StubUserClient(new User()),
                orderGateway,
                true);
        String idempotencyKey = UUID.randomUUID().toString();

        MercadoPagoOrderGateway.CheckoutOrder result = service.criarPedidoSandbox(
                "  Serviço de teste  ",
                new BigDecimal("25.50"),
                idempotencyKey);

        assertEquals("test-order-id", result.orderId());
        assertEquals("https://www.mercadopago.com.br/checkout/test", result.checkoutUrl());
        assertEquals("APP-sandbox-token", orderGateway.accessToken);
        assertEquals(UUID.fromString(idempotencyKey), orderGateway.idempotencyKey);
        assertEquals("Serviço de teste", orderGateway.description);
        assertEquals(new BigDecimal("25.50"), orderGateway.amount);
    }

    @Test
    void doesNotCreateOrdersWhenSandboxTestIsDisabled() {
        StubOrderGateway orderGateway = new StubOrderGateway();
        MercadoPagoPaymentService service = new MercadoPagoPaymentService(
                "APP-sandbox-token",
                new StubUserClient(new User()),
                orderGateway);

        WebApplicationException exception = assertThrows(
                WebApplicationException.class,
                () -> service.criarPedidoSandbox(
                        "Serviço de teste",
                        new BigDecimal("25.50"),
                        UUID.randomUUID().toString()));

        assertEquals(503, exception.getResponse().getStatus());
        assertFalse(orderGateway.called);
    }

    @Test
    void rejectsAmountsWithMoreThanTwoDecimalPlaces() {
        MercadoPagoPaymentService service = new MercadoPagoPaymentService(
                "APP-sandbox-token",
                new StubUserClient(new User()),
                new StubOrderGateway(),
                true);

        WebApplicationException exception = assertThrows(
                WebApplicationException.class,
                () -> service.criarPedidoSandbox(
                        "Serviço de teste",
                        new BigDecimal("25.501"),
                        UUID.randomUUID().toString()));

        assertEquals(400, exception.getResponse().getStatus());
    }

    private static class StubUserClient extends UserClient {
        private final User user;
        private MPRequestOptions options;
        private boolean called;

        private StubUserClient(User user) {
            this.user = user;
        }

        @Override
        public User get(MPRequestOptions options) {
            this.options = options;
            called = true;
            return user;
        }
    }

    private static class FailingUserClient extends UserClient {
        @Override
        public User get(MPRequestOptions options) throws MPException {
            throw new MPException("SDK communication failure");
        }
    }

    private static class StubOrderGateway implements MercadoPagoOrderGateway {
        private String accessToken;
        private UUID idempotencyKey;
        private String description;
        private BigDecimal amount;
        private boolean called;

        @Override
        public CheckoutOrder criarPedido(
                String accessToken,
                UUID idempotencyKey,
                String description,
                BigDecimal amount) {
            this.accessToken = accessToken;
            this.idempotencyKey = idempotencyKey;
            this.description = description;
            this.amount = amount;
            called = true;
            return new CheckoutOrder(
                    "test-order-id",
                    "https://www.mercadopago.com.br/checkout/test");
        }
    }
}
