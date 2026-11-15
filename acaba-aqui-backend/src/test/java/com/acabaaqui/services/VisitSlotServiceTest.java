package com.acabaaqui.services;

import com.acabaaqui.model.Conversation;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.model.User;
import com.acabaaqui.model.VisitSlot;
import com.acabaaqui.model.VisitSlotStatus;
import com.acabaaqui.persistence.ConversationRepository;
import com.acabaaqui.persistence.RequestOpportunityRepository;
import com.acabaaqui.persistence.ServiceRequestRepository;
import com.acabaaqui.persistence.UserRepository;
import com.acabaaqui.persistence.VisitSlotRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VisitSlotServiceTest {

    @Test
    void createsFutureWholeHourAvailabilityAndRejectsInvalidInput() {
        ServiceRequest request = request(42L, 7, ServiceRequestStatus.CRIADA);
        User client = request.getUsuario();
        FakeSlotRepository slots = new FakeSlotRepository();
        RecordingHistory history = new RecordingHistory();
        VisitSlotService service = service(request, slots, history);
        LocalDateTime tomorrow = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))
                .plusDays(1).withMinute(0).withSecond(0).withNano(0);

        service.criarDisponibilidades(request, List.of(
                new VisitSlotService.AvailabilityRequest(tomorrow, "America/Sao_Paulo"),
                new VisitSlotService.AvailabilityRequest(tomorrow.plusHours(2), "America/Sao_Paulo")), client);

        assertEquals(2, slots.records.size());
        assertEquals(VisitSlotStatus.DISPONIVEL, slots.records.get(0).getStatus());
        assertEquals(2, history.transitions.size());
        assertThrows(IllegalArgumentException.class, () -> service.criarDisponibilidades(
                request,
                List.of(new VisitSlotService.AvailabilityRequest(tomorrow.plusHours(4).plusMinutes(15),
                        "America/Sao_Paulo")),
                client));
        assertThrows(IllegalArgumentException.class, () -> service.criarDisponibilidades(
                request,
                List.of(new VisitSlotService.AvailabilityRequest(tomorrow.plusHours(4), "Not/AZone")),
                client));
        assertEquals(2, slots.records.size());
    }

    @Test
    void reservationRequiresAcceptedOpportunityAndPreventsCompetingProviders() {
        ServiceRequest request = request(42L, 7, ServiceRequestStatus.CRIADA);
        User firstProvider = user(11);
        User secondProvider = user(12);
        FakeSlotRepository slots = new FakeSlotRepository();
        VisitSlot first = slot(101L, request, firstFutureHour(), VisitSlotStatus.DISPONIVEL, null);
        VisitSlot second = slot(102L, request, firstFutureHour().plusHours(2),
                VisitSlotStatus.DISPONIVEL, null);
        slots.records.addAll(List.of(first, second));
        FakeOpportunityRepository opportunities = new FakeOpportunityRepository();
        opportunities.accepted.add("42:11");
        opportunities.accepted.add("42:12");
        VisitSlotService service = service(request, slots, new RecordingHistory(), opportunities);

        assertNull(service.reservar(101L, 13));
        assertNull(service.listar(42L, 13, false, true));
        assertEquals(VisitSlotStatus.RESERVADA, service.reservar(101L, firstProvider.getId()).getStatus());
        assertThrows(IllegalStateException.class, () -> service.reservar(101L, secondProvider.getId()));
        assertEquals(VisitSlotStatus.RESERVADA, service.reservar(102L, secondProvider.getId()).getStatus());
    }

    @Test
    void providerHasOneActiveSlotAndCancellationMakesFutureSlotAvailableAgain() {
        ServiceRequest request = request(42L, 7, ServiceRequestStatus.CRIADA);
        User provider = user(11);
        FakeSlotRepository slots = new FakeSlotRepository();
        VisitSlot first = slot(101L, request, firstFutureHour(), VisitSlotStatus.DISPONIVEL, null);
        VisitSlot second = slot(102L, request, firstFutureHour().plusHours(2),
                VisitSlotStatus.DISPONIVEL, null);
        slots.records.addAll(List.of(first, second));
        FakeOpportunityRepository opportunities = new FakeOpportunityRepository();
        opportunities.accepted.add("42:11");
        RecordingHistory history = new RecordingHistory();
        VisitSlotService service = service(request, slots, history, opportunities);

        service.reservar(101L, provider.getId());
        assertThrows(IllegalStateException.class, () -> service.reservar(102L, provider.getId()));
        VisitSlot cancelled = service.cancelar(101L, provider.getId(), false, true);

        assertEquals(VisitSlotStatus.DISPONIVEL, cancelled.getStatus());
        assertNull(cancelled.getPrestador());
        assertEquals(VisitSlotStatus.RESERVADA, service.reservar(102L, provider.getId()).getStatus());
        assertEquals("CANCELADA", history.transitions.get(1).newStatus());
        assertEquals("DISPONIVEL", history.transitions.get(2).newStatus());
    }

    @Test
    void clientConfirmsVisitAndPastReservationsExpireWhenSlotsAreListed() {
        ServiceRequest request = request(42L, 7, ServiceRequestStatus.CRIADA);
        User provider = user(11);
        FakeSlotRepository slots = new FakeSlotRepository();
        VisitSlot reserved = slot(101L, request, firstFutureHour(), VisitSlotStatus.RESERVADA, provider);
        VisitSlot past = slot(102L, request, LocalDateTime.now(ZoneId.of("UTC")).minusHours(2),
                VisitSlotStatus.RESERVADA, provider);
        VisitSlot pastConfirmed = slot(103L, request,
                LocalDateTime.now(ZoneId.of("UTC")).minusHours(2), VisitSlotStatus.CONFIRMADA, provider);
        past.setTimeZoneId("UTC");
        slots.records.addAll(List.of(reserved, past, pastConfirmed));
        FakeOpportunityRepository opportunities = new FakeOpportunityRepository();
        opportunities.accepted.add("42:11");
        RecordingHistory history = new RecordingHistory();
        VisitSlotService service = service(request, slots, history, opportunities);
        FakeConversationRepository conversations = new FakeConversationRepository();
        service.conversationRepository = conversations;

        VisitSlotService.ConfirmationResult confirmation = service.confirmar(101L, 7);
        assertEquals(VisitSlotStatus.CONFIRMADA, confirmation.slot().getStatus());
        assertEquals(1L, confirmation.conversationId());
        assertEquals(confirmation.conversationId(), service.confirmar(101L, 7).conversationId());
        assertEquals(1, conversations.records.size());
        List<VisitSlot> listed = service.listar(42L, 7, true, false);

        assertEquals(VisitSlotStatus.EXPIRADA, listed.get(0).getStatus());
        assertNull(listed.get(0).getPrestador());
        assertEquals(VisitSlotStatus.EXPIRADA, listed.get(1).getStatus());
        assertNull(listed.get(1).getPrestador());
        assertEquals(provider.getId(), listed.get(1).getPrestadorConfirmado().getId());
        assertEquals(LifecycleStatusTransitionPolicy.SISTEMA, history.transitions.get(1).actorType());
        assertNull(service.listar(42L, 8, true, false));
    }

    private VisitSlotService service(
            ServiceRequest request,
            FakeSlotRepository slots,
            RecordingHistory history) {
        return service(request, slots, history, new FakeOpportunityRepository());
    }

    private VisitSlotService service(
            ServiceRequest request,
            FakeSlotRepository slots,
            RecordingHistory history,
            FakeOpportunityRepository opportunities) {
        VisitSlotService service = new VisitSlotService();
        service.slotRepository = slots;
        service.requestRepository = new FakeServiceRequestRepository(request);
        service.opportunityRepository = opportunities;
        service.userRepository = new FakeUserRepository(List.of(request.getUsuario(), user(11), user(12), user(13)));
        service.conversationRepository = new FakeConversationRepository();
        service.lifecycleStatusService = history;
        return service;
    }

    private LocalDateTime firstFutureHour() {
        return LocalDateTime.now(ZoneId.of("UTC")).plusDays(1)
                .withMinute(0).withSecond(0).withNano(0);
    }

    private ServiceRequest request(Long id, int clientId, ServiceRequestStatus status) {
        ServiceRequest request = new ServiceRequest();
        request.setId(id);
        request.setUsuario(user(clientId));
        request.setStatus(status);
        return request;
    }

    private VisitSlot slot(
            Long id,
            ServiceRequest request,
            LocalDateTime start,
            VisitSlotStatus status,
            User provider) {
        VisitSlot slot = new VisitSlot();
        slot.setId(id);
        slot.setSolicitacao(request);
        slot.setInicioLocal(start);
        slot.setTimeZoneId("UTC");
        slot.setStatus(status);
        slot.setPrestador(provider);
        return slot;
    }

    private User user(int id) {
        User user = new User();
        user.setId(id);
        user.setNome("User " + id);
        return user;
    }

    private static class FakeServiceRequestRepository extends ServiceRequestRepository {
        private final ServiceRequest request;

        FakeServiceRequestRepository(ServiceRequest request) {
            this.request = request;
        }

        @Override
        public ServiceRequest buscarPorId(Long id) {
            return request.getId().equals(id) ? request : null;
        }

        @Override
        public ServiceRequest buscarComBloqueio(Long id) {
            return request.getId().equals(id) ? request : null;
        }
    }

    private static class FakeSlotRepository extends VisitSlotRepository {
        private final List<VisitSlot> records = new ArrayList<>();

        @Override
        public VisitSlot buscarPorId(Long id) {
            return records.stream().filter(slot -> slot.getId().equals(id)).findFirst().orElse(null);
        }

        @Override
        public VisitSlot buscarComBloqueio(Long id) {
            return buscarPorId(id);
        }

        @Override
        public List<VisitSlot> listarPorSolicitacao(Long requestId) {
            return records.stream().filter(slot -> slot.getSolicitacao().getId().equals(requestId))
                    .sorted((first, second) -> first.getInicioLocal().compareTo(second.getInicioLocal())).toList();
        }

        @Override
        public List<VisitSlot> listarAtivasPorPrestador(Long requestId, Integer providerId) {
            return records.stream().filter(slot -> slot.getSolicitacao().getId().equals(requestId)
                    && slot.getPrestador() != null
                    && slot.getPrestador().getId().equals(providerId)
                    && (slot.getStatus() == VisitSlotStatus.RESERVADA
                    || slot.getStatus() == VisitSlotStatus.CONFIRMADA)).toList();
        }

        @Override
        public void persist(VisitSlot slot) {
            slot.setId((long) records.size() + 1);
            records.add(slot);
        }
    }

    private static class FakeOpportunityRepository extends RequestOpportunityRepository {
        private final Set<String> accepted = new HashSet<>();

        @Override
        public boolean temOportunidadeAceita(Long requestId, Integer providerId) {
            return accepted.contains(requestId + ":" + providerId);
        }
    }

    private static class FakeConversationRepository extends ConversationRepository {
        private final List<Conversation> records = new ArrayList<>();

        @Override
        public Conversation buscarPorSolicitacaoEPrestador(Long requestId, Integer providerId) {
            return records.stream().filter(conversation ->
                    conversation.getSolicitacao().getId().equals(requestId)
                            && conversation.getPrestador().getId().equals(providerId))
                    .findFirst().orElse(null);
        }

        @Override
        public void persist(Conversation conversation) {
            conversation.setId((long) records.size() + 1);
            records.add(conversation);
        }
    }

    private static class FakeUserRepository extends UserRepository {
        private final List<User> records;

        FakeUserRepository(List<User> records) {
            this.records = records;
        }

        @Override
        public User buscarUsuarioPorId(Integer id) {
            return records.stream().filter(user -> user.getId().equals(id)).findFirst().orElse(null);
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
            transitions.add(new RecordedTransition(entityId, previousStatus, newStatus, actorType));
        }
    }

    private record RecordedTransition(
            Long entityId,
            String previousStatus,
            String newStatus,
            String actorType) {
    }
}
