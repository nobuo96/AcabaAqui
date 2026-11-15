package com.acabaaqui.services;

import java.math.BigDecimal;
import java.util.UUID;

public interface MercadoPagoOrderGateway {

    CheckoutOrder criarPedido(
            String accessToken,
            UUID idempotencyKey,
            String description,
            BigDecimal amount);

    record CheckoutOrder(String orderId, String checkoutUrl) { }
}
