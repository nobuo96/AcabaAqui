package com.acabaaqui.services;

import com.acabaaqui.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceCancellationPolicyTest {

    @Test
    void requiresAReasonAndNormalizesWhitespace() {
        assertEquals("Mudança de planos", ServiceCancellationPolicy.validarMotivo("  Mudança de planos  "));
        assertThrows(IllegalArgumentException.class,
                () -> ServiceCancellationPolicy.validarMotivo("no"));
        assertThrows(IllegalArgumentException.class,
                () -> ServiceCancellationPolicy.validarMotivo("x".repeat(1001)));
    }

    @Test
    void onlyAllowsPaidCancellationBeforeExecutionStarts() {
        ServiceCancellationPolicy.validarCancelamento(PaymentStatus.PAGO, false);
        ServiceCancellationPolicy.validarCancelamento(PaymentStatus.REEMBOLSO_PENDENTE, false);
        assertThrows(IllegalStateException.class,
                () -> ServiceCancellationPolicy.validarCancelamento(PaymentStatus.PAGO, true));
        assertThrows(IllegalStateException.class,
                () -> ServiceCancellationPolicy.validarCancelamento(PaymentStatus.AGUARDANDO_PAGAMENTO, false));
        assertThrows(IllegalStateException.class,
                () -> ServiceCancellationPolicy.validarCancelamento(PaymentStatus.REEMBOLSADO, false));
    }
}
