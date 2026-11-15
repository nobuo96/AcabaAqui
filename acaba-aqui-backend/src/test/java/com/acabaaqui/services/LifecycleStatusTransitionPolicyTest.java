package com.acabaaqui.services;

import com.acabaaqui.model.RequestOpportunityStatus;
import com.acabaaqui.model.ServiceRequestStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
                LifecycleStatusTransitionPolicy.EXECUCAO,
                "EM_ANDAMENTO",
                "CONCLUIDA_PELO_PRESTADOR",
                LifecycleStatusTransitionPolicy.PRESTADOR));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.DISPUTA,
                "ABERTA",
                "ENCERRADA_REEMBOLSO",
                LifecycleStatusTransitionPolicy.ADMINISTRADOR));
        assertDoesNotThrow(() -> LifecycleStatusTransitionPolicy.validar(
                LifecycleStatusTransitionPolicy.GARANTIA,
                "ACIONADA",
                "REEMBOLSO_PENDENTE",
                LifecycleStatusTransitionPolicy.SISTEMA));
    }
}
