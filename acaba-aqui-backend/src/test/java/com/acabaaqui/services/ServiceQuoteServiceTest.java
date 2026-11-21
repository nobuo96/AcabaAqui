package com.acabaaqui.services;

import com.acabaaqui.model.ServiceQuote;
import com.acabaaqui.model.ServiceQuoteStatus;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.model.User;
import com.acabaaqui.model.VisitSlot;
import com.acabaaqui.model.VisitSlotStatus;
import com.acabaaqui.persistence.RequestOpportunityRepository;
import com.acabaaqui.persistence.ServiceQuoteRepository;
import com.acabaaqui.persistence.ServiceRequestRepository;
import com.acabaaqui.persistence.UserRepository;
import com.acabaaqui.persistence.VisitSlotRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceQuoteServiceTest {

    @Test
    void providerCanSendOnlyAfterConfirmedVisitAndOnlyOnce() {
        ServiceRequest request = request();
        User provider = user(11);
        FakeQuoteRepository quotes = new FakeQuoteRepository();
        FakeSlotRepository slots = new FakeSlotRepository();
        FakeOpportunityRepository opportunities = new FakeOpportunityRepository();
        opportunities.accepted.add("42:11");
        RecordingNotifications notifications = new RecordingNotifications();
        RecordingHistory history = new RecordingHistory();
        ServiceQuoteService service = service(request, quotes, slots, opportunities, notifications, history);

        assertThrows(IllegalArgumentException.class, () -> service.enviar(
                provider.getId(), request.getId(), new ServiceQuoteService.QuoteSubmission(
                        "Troca de torneira", new BigDecimal("125.50"), 0)));
        assertThrows(IllegalStateException.class, () -> service.enviar(
                provider.getId(), request.getId(), new ServiceQuoteService.QuoteSubmission(
                        "Troca de torneira", new BigDecimal("125.50"), 30)));

        slots.records.add(slot(request, provider, VisitSlotStatus.CONFIRMADA,
                LocalDateTime.now(ZoneOffset.UTC).plusHours(1)));
        assertThrows(IllegalStateException.class, () -> service.enviar(
                provider.getId(), request.getId(), new ServiceQuoteService.QuoteSubmission(
                        "Troca de torneira", new BigDecimal("125.50"), 30)));
        slots.records.clear();
        VisitSlot completed = slot(request, provider, VisitSlotStatus.EXPIRADA,
                LocalDateTime.now(ZoneOffset.UTC).minusHours(2));
        completed.setPrestador(null);
        completed.setPrestadorConfirmado(provider);
        slots.records.add(completed);
        ServiceQuote quote = service.enviar(
                provider.getId(), request.getId(), new ServiceQuoteService.QuoteSubmission(
                        "  Troca de torneira  ", new BigDecimal("125.5"), 30));

        assertEquals("Troca de torneira", quote.getDescricao());
        assertEquals(new BigDecimal("125.50"), quote.getValorTotal());
        assertEquals(30, quote.getGarantiaDias());
        assertEquals(ServiceQuoteStatus.ENVIADO, quote.getStatus());
        assertTrue(quote.getExpiraEm().isAfter(LocalDateTime.now()));
        assertEquals(List.of("ORCAMENTO_ENVIADO"), notifications.types);
        assertEquals(List.of(5), notifications.recipientIds);
        assertEquals(1, history.transitions.size());
        assertThrows(IllegalStateException.class, () -> service.enviar(
                provider.getId(), request.getId(), new ServiceQuoteService.QuoteSubmission(
                        "Outro orçamento", new BigDecimal("200"), 30)));
    }

    @Test
    void approvalCancelsOtherQuotesAndIsIdempotent() {
        ServiceRequest request = request();
        User firstProvider = user(11);
        User secondProvider = user(12);
        ServiceQuote first = quote(101L, request, firstProvider, ServiceQuoteStatus.ENVIADO);
        ServiceQuote second = quote(102L, request, secondProvider, ServiceQuoteStatus.ENVIADO);
        FakeQuoteRepository quotes = new FakeQuoteRepository();
        quotes.records.addAll(List.of(first, second));
        RecordingNotifications notifications = new RecordingNotifications();
        RecordingHistory history = new RecordingHistory();
        RecordingVisitSlotService visits = new RecordingVisitSlotService();
        ServiceQuoteService service = service(
                request, quotes, new FakeSlotRepository(), new FakeOpportunityRepository(),
                notifications, history, visits);

        ServiceQuote approved = service.aprovar(first.getId(), request.getUsuario().getId());
        ServiceQuote repeatedApproval = service.aprovar(first.getId(), request.getUsuario().getId());

        assertEquals(ServiceQuoteStatus.APROVADO, approved.getStatus());
        assertEquals(first.getId(), repeatedApproval.getId());
        assertEquals(ServiceQuoteStatus.CANCELADO, second.getStatus());
        assertEquals(List.of("ORCAMENTO_APROVADO", "ORCAMENTO_CANCELADO"), notifications.types);
        assertEquals(List.of(11, 12), notifications.recipientIds);
        assertEquals(1, visits.cancelledRequests.size());
        assertEquals(2, history.transitions.size());
        assertThrows(IllegalStateException.class,
                () -> service.aprovar(second.getId(), request.getUsuario().getId()));
    }

    @Test
    void rejectsQuoteAndKeepsItVisibleOnlyToItsParticipants() {
        ServiceRequest request = request();
        ServiceQuote quote = quote(101L, request, user(11), ServiceQuoteStatus.ENVIADO);
        FakeQuoteRepository quotes = new FakeQuoteRepository();
        quotes.records.add(quote);
        FakeOpportunityRepository opportunities = new FakeOpportunityRepository();
        opportunities.accepted.add("42:11");
        RecordingNotifications notifications = new RecordingNotifications();
        ServiceQuoteService service = service(
                request, quotes, new FakeSlotRepository(), opportunities, notifications, new RecordingHistory());

        assertNull(service.listar(request.getId(), 99, false, false));
        assertNull(service.recusar(quote.getId(), 99));
        assertEquals(List.of(quote), service.listar(request.getId(), 11, false, true));
        assertEquals(ServiceQuoteStatus.RECUSADO, service.recusar(quote.getId(), 5).getStatus());
        assertEquals(List.of("ORCAMENTO_RECUSADO"), notifications.types);
        assertEquals(List.of(11), notifications.recipientIds);
        assertThrows(IllegalStateException.class, () -> service.aprovar(quote.getId(), 5));
    }

    @Test
    void expiresQuotesAtTheDeadlineAndDoesNotRepeatNotifications() {
        ServiceRequest request = request();
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 12, 0);
        ServiceQuote quote = quote(101L, request, user(11), ServiceQuoteStatus.ENVIADO);
        quote.setExpiraEm(now);
        FakeQuoteRepository quotes = new FakeQuoteRepository();
        quotes.records.add(quote);
        RecordingNotifications notifications = new RecordingNotifications();
        RecordingHistory history = new RecordingHistory();
        ServiceQuoteService service = service(
                request, quotes, new FakeSlotRepository(), new FakeOpportunityRepository(),
                notifications, history);
        Clock clock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

        assertEquals(1, service.expirarVencidos(clock));
        assertEquals(ServiceQuoteStatus.EXPIRADO, quote.getStatus());
        assertEquals(2, notifications.types.size());
        assertFalse(service.expirarVencidos(clock) > 0);
        assertEquals(2, notifications.types.size());
        assertEquals(1, history.transitions.size());
        assertEquals(LifecycleStatusTransitionPolicy.SISTEMA, history.transitions.get(0).actorType());
    }

    @Test
    void requestCancellationCancelsSentAndApprovedQuotes() {
        ServiceRequest request = request();
        ServiceQuote sent = quote(101L, request, user(11), ServiceQuoteStatus.ENVIADO);
        ServiceQuote approved = quote(102L, request, user(12), ServiceQuoteStatus.APROVADO);
        ServiceQuote rejected = quote(103L, request, user(13), ServiceQuoteStatus.RECUSADO);
        FakeQuoteRepository quotes = new FakeQuoteRepository();
        quotes.records.addAll(List.of(sent, approved, rejected));
        RecordingNotifications notifications = new RecordingNotifications();
        RecordingHistory history = new RecordingHistory();
        ServiceQuoteService service = service(
                request, quotes, new FakeSlotRepository(), new FakeOpportunityRepository(),
                notifications, history);

        service.cancelarTodosPorSolicitacao(
                request, request.getUsuario(), LifecycleStatusTransitionPolicy.CLIENTE);

        assertEquals(ServiceQuoteStatus.CANCELADO, sent.getStatus());
        assertEquals(ServiceQuoteStatus.CANCELADO, approved.getStatus());
        assertEquals(ServiceQuoteStatus.RECUSADO, rejected.getStatus());
        assertEquals(List.of(11, 12), notifications.recipientIds);
    }

    private ServiceQuoteService service(
            ServiceRequest request,
            FakeQuoteRepository quotes,
            FakeSlotRepository slots,
            FakeOpportunityRepository opportunities,
            RecordingNotifications notifications,
            RecordingHistory history) {
        return service(request, quotes, slots, opportunities, notifications, history, new RecordingVisitSlotService());
    }

    private ServiceQuoteService service(
            ServiceRequest request,
            FakeQuoteRepository quotes,
            FakeSlotRepository slots,
            FakeOpportunityRepository opportunities,
            RecordingNotifications notifications,
            RecordingHistory history,
            RecordingVisitSlotService visitSlotService) {
        ServiceQuoteService service = new ServiceQuoteService();
        service.quoteRepository = quotes;
        service.requestRepository = new FakeRequestRepository(request);
        service.opportunityRepository = opportunities;
        service.visitSlotRepository = slots;
        service.userRepository = new FakeUserRepository(List.of(request.getUsuario(), user(11), user(12), user(13)));
        service.notificationService = notifications;
        service.visitSlotService = visitSlotService;
        service.lifecycleStatusService = history;
        return service;
    }

    private ServiceRequest request() {
        ServiceRequest request = new ServiceRequest();
        request.setId(42L);
        request.setUsuario(user(5));
        request.setStatus(ServiceRequestStatus.CRIADA);
        return request;
    }

    private ServiceQuote quote(Long id, ServiceRequest request, User provider, ServiceQuoteStatus status) {
        ServiceQuote quote = new ServiceQuote();
        quote.setId(id);
        quote.setSolicitacao(request);
        quote.setPrestador(provider);
        quote.setDescricao("Serviço");
        quote.setValorTotal(new BigDecimal("100.00"));
        quote.setGarantiaDias(30);
        quote.setStatus(status);
        quote.setExpiraEm(LocalDateTime.now().plusDays(1));
        return quote;
    }

    private VisitSlot slot(
            ServiceRequest request,
            User provider,
            VisitSlotStatus status,
            LocalDateTime start) {
        VisitSlot slot = new VisitSlot();
        slot.setSolicitacao(request);
        slot.setPrestador(provider);
        slot.setStatus(status);
        slot.setInicioLocal(start);
        slot.setTimeZoneId("UTC");
        return slot;
    }

    private User user(int id) {
        User user = new User();
        user.setId(id);
        user.setNome("User " + id);
        return user;
    }

    private static class FakeQuoteRepository extends ServiceQuoteRepository {
        private final List<ServiceQuote> records = new ArrayList<>();

        @Override
        public Long buscarSolicitacaoId(Long quoteId) {
            return records.stream().filter(quote -> quote.getId().equals(quoteId))
                    .map(quote -> quote.getSolicitacao().getId()).findFirst().orElse(null);
        }

        @Override
        public List<ServiceQuote> listarPorSolicitacaoComBloqueio(Long requestId) {
            return records.stream()
                    .filter(quote -> quote.getSolicitacao().getId().equals(requestId))
                    .toList();
        }

        @Override
        public void persist(ServiceQuote quote) {
            quote.setId((long) records.size() + 1);
            records.add(quote);
        }

        @Override
        public List<ExpirationCandidate> listarExpiradosAte(LocalDateTime cutoff) {
            return records.stream()
                    .filter(quote -> quote.getStatus() == ServiceQuoteStatus.ENVIADO)
                    .filter(quote -> !quote.getExpiraEm().isAfter(cutoff))
                    .map(quote -> new ExpirationCandidate(
                            quote.getId(), quote.getSolicitacao().getId()))
                    .toList();
        }
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

    private static class FakeOpportunityRepository extends RequestOpportunityRepository {
        private final Set<String> accepted = new HashSet<>();

        @Override
        public boolean temOportunidadeAceita(Long requestId, Integer providerId) {
            return accepted.contains(requestId + ":" + providerId);
        }
    }

    private static class FakeSlotRepository extends VisitSlotRepository {
        private final List<VisitSlot> records = new ArrayList<>();

        @Override
        public List<VisitSlot> listarPorSolicitacao(Long requestId) {
            return records.stream()
                    .filter(slot -> slot.getSolicitacao().getId().equals(requestId))
                    .toList();
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

    private static class RecordingNotifications extends InAppNotificationService {
        private final List<Integer> recipientIds = new ArrayList<>();
        private final List<String> types = new ArrayList<>();

        @Override
        public void criar(User recipient, ServiceRequest request, ServiceQuote quote, String type, String message) {
            recipientIds.add(recipient.getId());
            types.add(type);
        }
    }

    private static class RecordingVisitSlotService extends VisitSlotService {
        private final List<ServiceRequest> cancelledRequests = new ArrayList<>();

        @Override
        public void cancelarTodosPorSolicitacao(ServiceRequest request, User actor, String actorType) {
            cancelledRequests.add(request);
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
            transitions.add(new RecordedTransition(entityType, previousStatus, newStatus, actorType));
        }
    }

    private record RecordedTransition(
            String entityType,
            String previousStatus,
            String newStatus,
            String actorType) { }
}
