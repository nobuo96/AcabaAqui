package com.acabaaqui.services;

import com.acabaaqui.model.ServiceWarrantyStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceWarrantyPolicyTest {

    @Test
    void warrantyCanOnlyBeOpenedWithinItsConfiguredWindow() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime end = start.plusDays(30);

        assertDoesNotThrow(() -> ServiceWarrantyPolicy.validarAbertura(
                ServiceWarrantyStatus.ATIVA, start, end, start));
        assertDoesNotThrow(() -> ServiceWarrantyPolicy.validarAbertura(
                ServiceWarrantyStatus.ATIVA, start, end, end));
        assertThrows(IllegalStateException.class, () -> ServiceWarrantyPolicy.validarAbertura(
                ServiceWarrantyStatus.ATIVA, start, end, end.plusNanos(1)));
        assertThrows(IllegalStateException.class, () -> ServiceWarrantyPolicy.validarAbertura(
                ServiceWarrantyStatus.RESOLVIDA, start, end, start));
    }

    @Test
    void correctionDeadlineIsSevenDaysAndResolutionRequiresAnOpenClaim() {
        LocalDateTime openedAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime deadline = ServiceWarrantyPolicy.prazoCorrecao(openedAt);

        assertEquals(openedAt.plusDays(7), deadline);
        assertDoesNotThrow(() -> ServiceWarrantyPolicy.validarCorrecao(
                ServiceWarrantyStatus.ACIONADA, deadline, deadline.minusNanos(1)));
        assertThrows(IllegalStateException.class, () -> ServiceWarrantyPolicy.validarCorrecao(
                ServiceWarrantyStatus.ACIONADA, deadline, deadline));
        assertDoesNotThrow(() -> ServiceWarrantyPolicy.validarResolucao(
                ServiceWarrantyStatus.ACIONADA, deadline, deadline.minusNanos(1)));
        assertDoesNotThrow(() -> ServiceWarrantyPolicy.validarResolucao(
                ServiceWarrantyStatus.CORRIGIDA, deadline, deadline.plusDays(1)));
        assertThrows(IllegalStateException.class, () -> ServiceWarrantyPolicy.validarResolucao(
                ServiceWarrantyStatus.ACIONADA, deadline, deadline));
    }

    @Test
    void normalizesReasonAndRejectsShortOrMissingText() {
        assertEquals("Problema no serviço", ServiceWarrantyPolicy.validarMotivo("  Problema no serviço  "));
        assertThrows(IllegalArgumentException.class, () -> ServiceWarrantyPolicy.validarMotivo("abc"));
        assertThrows(IllegalArgumentException.class, () -> ServiceWarrantyPolicy.validarMotivo(null));
    }

    @Test
    void allowsOnlyExpectedWarrantyAndPostPayoutRefundTransitions() {
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.GARANTIA, null, "ATIVA",
                LifecycleStatusTransitionPolicy.SISTEMA));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.GARANTIA, "ATIVA", "ACIONADA",
                LifecycleStatusTransitionPolicy.CLIENTE));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.GARANTIA, "ACIONADA", "CORRIGIDA",
                LifecycleStatusTransitionPolicy.PRESTADOR));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.GARANTIA, "REEMBOLSO_PENDENTE", "REEMBOLSADA",
                LifecycleStatusTransitionPolicy.MERCADO_PAGO));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.PAGAMENTO, "REPASSADO", "REEMBOLSO_PENDENTE",
                LifecycleStatusTransitionPolicy.SISTEMA));
        assertThrows(IllegalStateException.class, () -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.GARANTIA, "ATIVA", "CORRIGIDA",
                LifecycleStatusTransitionPolicy.PRESTADOR));
    }
}
