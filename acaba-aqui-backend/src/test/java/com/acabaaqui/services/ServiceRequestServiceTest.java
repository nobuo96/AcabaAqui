package com.acabaaqui.services;

import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.RequestOpportunityRepository;
import com.acabaaqui.persistence.ServiceRequestRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceRequestServiceTest {

    @Test
    void normalizesPositiveAreaToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("12.50"), ServiceRequestService.validarArea(new BigDecimal("12.5")));
        assertEquals(new BigDecimal("12.30"), ServiceRequestService.validarArea(new BigDecimal("12.3000")));
    }

    @Test
    void rejectsNonPositiveArea() {
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestService.validarArea(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestService.validarArea(new BigDecimal("-1")));
    }

    @Test
    void rejectsAreaWithMoreThanTwoMeaningfulDecimalsOrTooManyDigits() {
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestService.validarArea(new BigDecimal("1.234")));
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestService.validarArea(new BigDecimal("100000000")));
    }

    @Test
    void cancellationClosesAnOwnedRequest() {
        ServiceRequest request = request(42, ServiceRequestStatus.CRIADA);
        List<ServiceRequest> cancelledVisitRequests = new ArrayList<>();
        ServiceRequestService service = new ServiceRequestService();
        service.requestRepository = new FakeRequestRepository(request);
        RecordingLifecycleStatusService history = new RecordingLifecycleStatusService();
        service.closureService = closureService(history, cancelledVisitRequests);

        ServiceRequest cancelled = service.cancelar(7, 42L);

        assertEquals(ServiceRequestStatus.ENCERRADA, cancelled.getStatus());
        assertEquals(1, history.transitions.size());
        assertEquals(LifecycleStatusTransitionPolicy.CLIENTE, history.transitions.get(0).actorType());
        assertEquals(List.of(request), cancelledVisitRequests);
    }

    @Test
    void cancellationDoesNotExposeAnotherUsersRequestAndCannotRepeat() {
        ServiceRequest request = request(42, ServiceRequestStatus.CRIADA);
        ServiceRequestService service = new ServiceRequestService();
        service.requestRepository = new FakeRequestRepository(request);
        RecordingLifecycleStatusService history = new RecordingLifecycleStatusService();
        service.closureService = closureService(history, new ArrayList<>());

        assertNull(service.cancelar(8, 42L));
        service.cancelar(7, 42L);
        assertThrows(IllegalStateException.class, () -> service.cancelar(7, 42L));
        assertEquals(1, history.transitions.size());
    }

    private ServiceRequestClosureService closureService(
            RecordingLifecycleStatusService history,
            List<ServiceRequest> cancelledVisitRequests) {
        ServiceRequestClosureService closure = new ServiceRequestClosureService();
        closure.opportunityRepository = new RequestOpportunityRepository() {
            @Override
            public List<com.acabaaqui.model.RequestOpportunity> listarPorSolicitacaoComBloqueio(Long requestId) {
                return List.of();
            }
        };
        closure.lifecycleStatusService = history;
        closure.quoteService = new ServiceQuoteService() {
            @Override
            public void cancelarTodosPorSolicitacao(ServiceRequest request, User actor, String actorType) {
            }
        };
        closure.visitSlotService = new VisitSlotService() {
            @Override
            public void cancelarTodosPorSolicitacao(ServiceRequest request, User actor, String actorType) {
                cancelledVisitRequests.add(request);
            }
        };
        return closure;
    }

    private ServiceRequest request(long id, ServiceRequestStatus status) {
        User user = new User();
        user.setId(7);
        ServiceRequest request = new ServiceRequest();
        request.setId(id);
        request.setUsuario(user);
        request.setStatus(status);
        return request;
    }

    private static class FakeRequestRepository extends ServiceRequestRepository {
        private final ServiceRequest request;

        FakeRequestRepository(ServiceRequest request) {
            this.request = request;
        }

        @Override
        public ServiceRequest buscarComBloqueio(Long id) {
            return request.getId().equals(id) ? request : null;
        }
    }

    private static class RecordingLifecycleStatusService extends LifecycleStatusService {
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
            transitions.add(new RecordedTransition(
                    entityType, entityId, previousStatus, newStatus, actorType, actor));
        }
    }

    private record RecordedTransition(
            String entityType,
            Long entityId,
            String previousStatus,
            String newStatus,
            String actorType,
            User actor) { }
}
