package com.acabaaqui.services;

import com.acabaaqui.model.RequestOpportunityStatus;
import com.acabaaqui.model.PaymentStatus;
import com.acabaaqui.model.ServiceRequestStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleStatusTransitionPolicyTest {

    @Test
    void allowsExistingRequestAndOpportunityTransitionsForTheirActors() {
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.SOLICITACAO,
                null,
                ServiceRequestStatus.CRIADA.name(),
                LifecycleStatusTransitionPolicy.CLIENTE));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.SOLICITACAO,
                ServiceRequestStatus.CRIADA.name(),
                ServiceRequestStatus.ENCERRADA.name(),
                LifecycleStatusTransitionPolicy.CLIENTE));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.OPORTUNIDADE,
                RequestOpportunityStatus.PENDENTE.name(),
                RequestOpportunityStatus.ACEITA.name(),
                LifecycleStatusTransitionPolicy.PRESTADOR));
    }

    @Test
    void rejectsTransitionsByTheWrongActorAndTransitionsFromTerminalStates() {
        assertThrows(IllegalStateException.class, () -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.SOLICITACAO,
                ServiceRequestStatus.CRIADA.name(),
                ServiceRequestStatus.ENCERRADA.name(),
                LifecycleStatusTransitionPolicy.PRESTADOR));
        assertThrows(IllegalStateException.class, () -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.SOLICITACAO,
                ServiceRequestStatus.ENCERRADA.name(),
                ServiceRequestStatus.CRIADA.name(),
                LifecycleStatusTransitionPolicy.CLIENTE));
        assertThrows(IllegalStateException.class, () -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.OPORTUNIDADE,
                RequestOpportunityStatus.ACEITA.name(),
                RequestOpportunityStatus.RECUSADA.name(),
                LifecycleStatusTransitionPolicy.PRESTADOR));
    }

    @Test
    void legacyRequestStatusesRemainReadable() {
        assertEquals(ServiceRequestStatus.CRIADA, ServiceRequestStatus.valueOf("CRIADA"));
        assertEquals(ServiceRequestStatus.ENCERRADA, ServiceRequestStatus.valueOf("ENCERRADA"));
    }

    @Test
    void validatesTransitionsForPlannedLifecycleDomains() {
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.VISITA,
                "RESERVADA",
                "CONFIRMADA",
                LifecycleStatusTransitionPolicy.CLIENTE));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.ORCAMENTO,
                "ENVIADO",
                "APROVADO",
                LifecycleStatusTransitionPolicy.CLIENTE));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.PAGAMENTO,
                "AGUARDANDO_PAGAMENTO",
                "PAGO",
                LifecycleStatusTransitionPolicy.MERCADO_PAGO));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.PAGAMENTO,
                "PAGO",
                "CONTESTADO",
                LifecycleStatusTransitionPolicy.MERCADO_PAGO));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.PAGAMENTO,
                "REEMBOLSO_PENDENTE",
                "REEMBOLSADO",
                LifecycleStatusTransitionPolicy.MERCADO_PAGO));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.EXECUCAO,
                "EM_ANDAMENTO",
                "CONCLUIDA_PELO_PRESTADOR",
                LifecycleStatusTransitionPolicy.PRESTADOR));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.EXECUCAO,
                "EM_ANDAMENTO",
                "CONTESTADA",
                LifecycleStatusTransitionPolicy.CLIENTE));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.EXECUCAO,
                "CONTESTADA",
                "APROVADA",
                LifecycleStatusTransitionPolicy.ADMINISTRADOR));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.EXECUCAO,
                "AGUARDANDO_INICIO",
                "CANCELADA",
                LifecycleStatusTransitionPolicy.CLIENTE));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.DISPUTA,
                "ABERTA",
                "ENCERRADA_REEMBOLSO",
                LifecycleStatusTransitionPolicy.ADMINISTRADOR));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.DISPUTA,
                "ABERTA",
                "ENCERRADA_REPASSE",
                LifecycleStatusTransitionPolicy.ADMINISTRADOR));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.GARANTIA,
                "ACIONADA",
                "REEMBOLSO_PENDENTE",
                LifecycleStatusTransitionPolicy.SISTEMA));
    }

    @Test
    void allowsCustomerRetryAfterTerminalCheckoutAndPreventsPaymentStatusRegression() {
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.PAGAMENTO,
                PaymentStatus.RECUSADO.name(),
                PaymentStatus.AGUARDANDO_PAGAMENTO.name(),
                LifecycleStatusTransitionPolicy.CLIENTE));
        assertThrows(IllegalStateException.class, () -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.PAGAMENTO,
                PaymentStatus.RECUSADO.name(),
                PaymentStatus.AGUARDANDO_PAGAMENTO.name(),
                LifecycleStatusTransitionPolicy.MERCADO_PAGO));

        assertTrue(ServicePaymentService.podeTransicionar(
                PaymentStatus.AGUARDANDO_PAGAMENTO, PaymentStatus.PAGO));
        assertFalse(ServicePaymentService.podeTransicionar(
                PaymentStatus.PAGO, PaymentStatus.AGUARDANDO_PAGAMENTO));
        assertFalse(ServicePaymentService.podeTransicionar(
                PaymentStatus.REEMBOLSADO, PaymentStatus.PAGO));
        assertEquals(PaymentStatus.EXPIRADO, ServicePaymentService.mapStatus(
                new MercadoPagoOrderGateway.OrderSnapshot(
                        "order-id", "reference", "failed", "expired", null)));
        assertEquals(PaymentStatus.CONTESTADO, ServicePaymentService.mapStatus(
                new MercadoPagoOrderGateway.OrderSnapshot(
                        "order-id", "reference", "charged_back", "in_process", null)));
        assertEquals(PaymentStatus.REEMBOLSADO_PARCIAL, ServicePaymentService.mapStatus(
                new MercadoPagoOrderGateway.OrderSnapshot(
                        "order-id", "reference", "processed", "partially_refunded", null)));
    }
}
