package com.acabaaqui.services;

import com.acabaaqui.model.PaymentStatus;
import com.acabaaqui.model.ServiceExecution;
import com.acabaaqui.model.ServiceExecutionStatus;
import com.acabaaqui.model.ServicePayoutReason;
import com.acabaaqui.model.ServicePayoutStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicePayoutPolicyTest {

    @Test
    void payoutBecomesEligibleAfterApprovalOrAtTheEndOfTheReviewWindow() {
        LocalDateTime deadline = LocalDateTime.of(2026, 3, 8, 10, 0);
        ServiceExecution execution = new ServiceExecution();
        execution.setStatus(ServiceExecutionStatus.CONCLUIDA_PELO_PRESTADOR);
        execution.setAprovarAte(deadline);
        assertNull(ServicePayoutPolicy.motivoElegibilidade(execution, deadline.minusNanos(1)));
        assertEquals(ServicePayoutReason.PRAZO_EXPIRADO,
                ServicePayoutPolicy.motivoElegibilidade(execution, deadline));

        execution.setStatus(ServiceExecutionStatus.APROVADA);
        assertEquals(ServicePayoutReason.APROVACAO_CLIENTE,
                ServicePayoutPolicy.motivoElegibilidade(execution, deadline.minusDays(2)));
    }

    @Test
    void pendingPayoutRequiresConfirmedPaymentAndNoOpenDispute() {
        assertDoesNotThrow(() -> ServicePayoutPolicy.validarCriacao(
                PaymentStatus.PAGO, false, ServicePayoutReason.APROVACAO_CLIENTE));
        assertThrows(IllegalStateException.class, () -> ServicePayoutPolicy.validarCriacao(
                PaymentStatus.REEMBOLSO_PENDENTE, false, ServicePayoutReason.APROVACAO_CLIENTE));
        assertThrows(IllegalStateException.class, () -> ServicePayoutPolicy.validarCriacao(
                PaymentStatus.PAGO, true, ServicePayoutReason.PRAZO_EXPIRADO));
    }

    @Test
    void manualPaymentIsBoundedByThePaidAmountAndCannotBeRepeated() {
        BigDecimal amount = ServicePayoutPolicy.validarPagamento(
                ServicePayoutStatus.PENDENTE,
                PaymentStatus.REPASSE_PENDENTE,
                false,
                new BigDecimal("95.50"),
                new BigDecimal("100.00"),
                "PIX-123");
        assertEquals(new BigDecimal("95.50"), amount);

        assertThrows(IllegalArgumentException.class, () -> ServicePayoutPolicy.validarPagamento(
                ServicePayoutStatus.PENDENTE,
                PaymentStatus.REPASSE_PENDENTE,
                false,
                new BigDecimal("100.01"),
                new BigDecimal("100.00"),
                "PIX-124"));
        assertThrows(IllegalStateException.class, () -> ServicePayoutPolicy.validarPagamento(
                ServicePayoutStatus.PAGO,
                PaymentStatus.REPASSADO,
                false,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                "PIX-125"));
    }

    @Test
    void paymentReconciliationCannotReopenAPayoutAndCanBlockItForChargeback() {
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.PAGAMENTO,
                PaymentStatus.PAGO.name(),
                PaymentStatus.REPASSE_PENDENTE.name(),
                LifecycleStatusTransitionPolicy.SISTEMA));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.PAGAMENTO,
                PaymentStatus.REPASSE_PENDENTE.name(),
                PaymentStatus.REPASSADO.name(),
                LifecycleStatusTransitionPolicy.ADMINISTRADOR));
        assertTrue(ServicePaymentService.podeTransicionar(
                PaymentStatus.REPASSE_PENDENTE, PaymentStatus.CONTESTADO));
        assertTrue(ServicePaymentService.podeTransicionar(
                PaymentStatus.REPASSADO, PaymentStatus.REEMBOLSO_PENDENTE));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.PAGAMENTO,
                PaymentStatus.REPASSADO.name(),
                PaymentStatus.REEMBOLSO_PENDENTE.name(),
                LifecycleStatusTransitionPolicy.MERCADO_PAGO));
        assertThrows(IllegalStateException.class, () -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.PAGAMENTO,
                PaymentStatus.REPASSE_PENDENTE.name(),
                PaymentStatus.REPASSADO.name(),
                LifecycleStatusTransitionPolicy.SISTEMA));
    }
}
