package com.acabaaqui.services;

import java.math.BigDecimal;
import java.util.UUID;

public interface MercadoPagoOrderGateway {

    CheckoutOrder criarPedido(
            String accessToken,
            UUID idempotencyKey,
            String description,
            BigDecimal amount);

    CheckoutOrder criarPedido(
            String accessToken,
            UUID idempotencyKey,
            String externalReference,
            String description,
            BigDecimal amount,
            ReturnUrls returnUrls);

    OrderSnapshot consultarPedido(String accessToken, String orderId);

    OrderSnapshot cancelarPedido(String accessToken, String orderId, UUID idempotencyKey);

    OrderSnapshot reembolsarPedido(String accessToken, String orderId, UUID idempotencyKey);

    record CheckoutOrder(String orderId, String checkoutUrl) { }

    record ReturnUrls(String successUrl, String failureUrl, String pendingUrl) { }

    record OrderSnapshot(
            String orderId,
            String externalReference,
            String status,
            String statusDetail,
            BigDecimal totalPaidAmount) { }
}
