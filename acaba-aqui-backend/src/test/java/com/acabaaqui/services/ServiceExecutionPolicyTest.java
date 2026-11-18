package com.acabaaqui.services;

import com.acabaaqui.model.PaymentStatus;
import com.acabaaqui.model.ServiceExecutionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceExecutionPolicyTest {

    @Test
    void startRequiresPaidPaymentWithoutCancellation() {
        assertDoesNotThrow(() -> ServiceExecutionPolicy.validarInicio(
                ServiceExecutionStatus.AGUARDANDO_INICIO, PaymentStatus.PAGO, false));
        assertThrows(IllegalStateException.class, () -> ServiceExecutionPolicy.validarInicio(
                ServiceExecutionStatus.AGUARDANDO_INICIO, PaymentStatus.REEMBOLSO_PENDENTE, false));
        assertThrows(IllegalStateException.class, () -> ServiceExecutionPolicy.validarInicio(
                ServiceExecutionStatus.AGUARDANDO_INICIO, PaymentStatus.PAGO, true));
    }

    @Test
    void completionRequiresAnOngoingService() {
        assertDoesNotThrow(() -> ServiceExecutionPolicy.validarConclusao(
                ServiceExecutionStatus.EM_ANDAMENTO));
        assertThrows(IllegalStateException.class, () -> ServiceExecutionPolicy.validarConclusao(
                ServiceExecutionStatus.AGUARDANDO_INICIO));
    }

    @Test
    void approvalIsAllowedThroughTheDeadlineButNotAfterIt() {
        LocalDateTime deadline = LocalDateTime.of(2026, 1, 8, 12, 0);
        assertDoesNotThrow(() -> ServiceExecutionPolicy.validarAprovacao(
                ServiceExecutionStatus.CONCLUIDA_PELO_PRESTADOR, deadline, deadline));
        assertThrows(IllegalStateException.class, () -> ServiceExecutionPolicy.validarAprovacao(
                ServiceExecutionStatus.CONCLUIDA_PELO_PRESTADOR, deadline, deadline.plusNanos(1)));
        assertThrows(IllegalStateException.class, () -> ServiceExecutionPolicy.validarAprovacao(
                ServiceExecutionStatus.EM_ANDAMENTO, deadline, deadline.minusDays(1)));
    }
}
