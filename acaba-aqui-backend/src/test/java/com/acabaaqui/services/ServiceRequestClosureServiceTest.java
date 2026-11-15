package com.acabaaqui.services;

import com.acabaaqui.model.RequestOpportunity;
import com.acabaaqui.model.RequestOpportunityStatus;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.RequestOpportunityRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceRequestClosureServiceTest {

    @Test
    void closesRequestWhenAllOpportunitiesWereDeclined() {
        ServiceRequest request = request();
        FakeOpportunityRepository opportunities = new FakeOpportunityRepository();
        opportunities.records.addAll(List.of(
                opportunity(1, request, RequestOpportunityStatus.RECUSADA),
                opportunity(2, request, RequestOpportunityStatus.RECUSADA)));
        RecordingHistory history = new RecordingHistory();
        RecordingVisitSlotService visits = new RecordingVisitSlotService();
        ServiceRequestClosureService service = service(opportunities, history, visits);

        assertTrue(service.encerrarSeTodasRecusadas(request));
        assertFalse(service.encerrarSeTodasRecusadas(request));

        assertEquals(ServiceRequestStatus.ENCERRADA, request.getStatus());
        assertEquals(1, history.transitions.size());
        assertEquals(LifecycleStatusTransitionPolicy.SISTEMA, history.transitions.get(0).actorType());
        assertEquals(List.of(request), visits.closedRequests);
        assertEquals(List.of(LifecycleStatusTransitionPolicy.SISTEMA), visits.actorTypes);
    }

    @Test
    void doesNotCloseRequestWithPendingOrAcceptedOpportunitiesWhenCheckingAllDeclined() {
        ServiceRequest request = request();
        FakeOpportunityRepository opportunities = new FakeOpportunityRepository();
        opportunities.records.addAll(List.of(
                opportunity(1, request, RequestOpportunityStatus.RECUSADA),
                opportunity(2, request, RequestOpportunityStatus.PENDENTE)));
        ServiceRequestClosureService service = service(
                opportunities, new RecordingHistory(), new RecordingVisitSlotService());

        assertFalse(service.encerrarSeTodasRecusadas(request));
        opportunities.records.get(1).setStatus(RequestOpportunityStatus.ACEITA);
        assertFalse(service.encerrarSeTodasRecusadas(request));

        assertEquals(ServiceRequestStatus.CRIADA, request.getStatus());
    }

    @Test
    void expirationClosesPendingOpportunitiesAndKeepsAcceptedRequestsActive() {
        ServiceRequest request = request();
        FakeOpportunityRepository opportunities = new FakeOpportunityRepository();
        RequestOpportunity pending = opportunity(1, request, RequestOpportunityStatus.PENDENTE);
        opportunities.records.add(pending);
        RecordingHistory history = new RecordingHistory();
        ServiceRequestClosureService service = service(
                opportunities, history, new RecordingVisitSlotService());

        assertTrue(service.encerrarSeSemAceite(request));

        assertEquals(ServiceRequestStatus.ENCERRADA, request.getStatus());
        assertEquals(RequestOpportunityStatus.ENCERRADA, pending.getStatus());
        assertEquals(List.of("SOLICITACAO", "OPORTUNIDADE"),
                history.transitions.stream().map(RecordedTransition::entityType).toList());
        assertTrue(history.transitions.stream()
                .allMatch(transition -> transition.actorType().equals(LifecycleStatusTransitionPolicy.SISTEMA)));

        ServiceRequest acceptedRequest = request();
        FakeOpportunityRepository acceptedOpportunities = new FakeOpportunityRepository();
        acceptedOpportunities.records.add(
                opportunity(2, acceptedRequest, RequestOpportunityStatus.ACEITA));
        ServiceRequestClosureService acceptedService = service(
                acceptedOpportunities, new RecordingHistory(), new RecordingVisitSlotService());

        assertFalse(acceptedService.encerrarSeSemAceite(acceptedRequest));
        assertEquals(ServiceRequestStatus.CRIADA, acceptedRequest.getStatus());
    }

    private ServiceRequestClosureService service(
            FakeOpportunityRepository opportunities,
            RecordingHistory history,
            RecordingVisitSlotService visits) {
        ServiceRequestClosureService service = new ServiceRequestClosureService();
        service.opportunityRepository = opportunities;
        service.lifecycleStatusService = history;
        service.quoteService = new ServiceQuoteService() {
            @Override
            public void cancelarTodosPorSolicitacao(ServiceRequest request, User actor, String actorType) {
            }
        };
        service.visitSlotService = visits;
        return service;
    }

    private ServiceRequest request() {
        ServiceRequest request = new ServiceRequest();
        request.setId(42L);
        request.setStatus(ServiceRequestStatus.CRIADA);
        return request;
    }

    private RequestOpportunity opportunity(long id, ServiceRequest request, RequestOpportunityStatus status) {
        RequestOpportunity opportunity = new RequestOpportunity();
        opportunity.setId(id);
        opportunity.setSolicitacao(request);
        opportunity.setStatus(status);
        return opportunity;
    }

    private static class FakeOpportunityRepository extends RequestOpportunityRepository {
        private final List<RequestOpportunity> records = new ArrayList<>();

        @Override
        public List<RequestOpportunity> listarPorSolicitacaoComBloqueio(Long requestId) {
            return records.stream()
                    .filter(item -> item.getSolicitacao().getId().equals(requestId))
                    .toList();
        }
    }

    private static class RecordingVisitSlotService extends VisitSlotService {
        private final List<ServiceRequest> closedRequests = new ArrayList<>();
        private final List<String> actorTypes = new ArrayList<>();

        @Override
        public void cancelarTodosPorSolicitacao(ServiceRequest request, User actor, String actorType) {
            closedRequests.add(request);
            actorTypes.add(actorType);
        }
    }

    private static class RecordingHistory extends LifecycleStatusService {
        private final List<RecordedTransition> transitions = new ArrayList<>();

        @Override
        public void registrarTransicao(
                String entityType,
                Long entityId,
                String previousStatus,
                String newStatus,
                String actorType,
                User actor) {
            LifecycleStatusTransitionPolicy.validar(entityType, previousStatus, newStatus, actorType);
            transitions.add(new RecordedTransition(entityType, actorType));
        }
    }

    private record RecordedTransition(String entityType, String actorType) {
    }
}
