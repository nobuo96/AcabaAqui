package com.acabaaqui.services;

import com.acabaaqui.model.Conversation;
import com.acabaaqui.model.RequestOpportunity;
import com.acabaaqui.model.RequestOpportunityStatus;
import com.acabaaqui.model.ServiceCatalogItem;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.ConversationRepository;
import com.acabaaqui.persistence.ProviderRegionRepository;
import com.acabaaqui.persistence.RequestOpportunityRepository;
import com.acabaaqui.persistence.ServiceRequestRepository;
import com.acabaaqui.persistence.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class RequestOpportunityServiceTest {

    @Test
    void createsOneInAppOpportunityPerProviderMatchedByServiceAndCityUf() {
        User firstProvider = user(11);
        User secondProvider = user(12);
        FakeCoverageRepository coverage = new FakeCoverageRepository(List.of(firstProvider, secondProvider));
        FakeOpportunityRepository opportunities = new FakeOpportunityRepository();
        RequestOpportunityService service = new RequestOpportunityService();
        service.areaRepository = coverage;
        service.opportunityRepository = opportunities;

        ServiceRequest request = request();
        service.notificarCorrespondentes(request);

        assertEquals("Campinas", coverage.city);
        assertEquals("SP", coverage.uf);
        assertEquals(2, opportunities.records.size());
        assertEquals(List.of(11, 12), opportunities.records.stream()
                .map(item -> item.getPrestador().getId()).toList());
        assertEquals(RequestOpportunityStatus.PENDENTE, opportunities.records.get(0).getStatus());
    }

    @Test
    void multipleProvidersCanAcceptAndRepeatedAcceptanceReturnsTheSameConversation() {
        User client = user(5);
        User firstProvider = user(11);
        User secondProvider = user(12);
        ServiceRequest request = request();
        request.setUsuario(client);
        FakeOpportunityRepository opportunities = new FakeOpportunityRepository();
        RequestOpportunity first = opportunity(101, request, firstProvider);
        RequestOpportunity second = opportunity(102, request, secondProvider);
        opportunities.records.addAll(List.of(first, second));
        FakeConversationRepository conversations = new FakeConversationRepository();
        FakeUserRepository users = new FakeUserRepository(List.of(firstProvider, secondProvider));
        RequestOpportunityService service = new RequestOpportunityService();
        service.opportunityRepository = opportunities;
        service.requestRepository = new FakeServiceRequestRepository(request);
        service.conversationRepository = conversations;
        service.userRepository = users;

        RequestOpportunityService.AcceptedConversation firstChat = service.aceitar(11, 101L);
        RequestOpportunityService.AcceptedConversation secondChat = service.aceitar(12, 102L);
        RequestOpportunityService.AcceptedConversation repeatedFirstAcceptance = service.aceitar(11, 101L);

        assertNotSame(firstChat, secondChat);
        assertEquals(firstChat.id(), repeatedFirstAcceptance.id());
        assertEquals(2, conversations.records.size());
        assertEquals(RequestOpportunityStatus.ACEITA, first.getStatus());
        assertEquals(RequestOpportunityStatus.ACEITA, second.getStatus());
    }

    @Test
    void closingRequestOpportunitiesOnlyClosesPendingEntries() {
        ServiceRequest request = request();
        RequestOpportunity pending = opportunity(101, request, user(11));
        RequestOpportunity accepted = opportunity(102, request, user(12));
        accepted.setStatus(RequestOpportunityStatus.ACEITA);
        FakeOpportunityRepository opportunities = new FakeOpportunityRepository();
        opportunities.records.addAll(List.of(pending, accepted));
        RequestOpportunityService service = new RequestOpportunityService();
        service.opportunityRepository = opportunities;

        int closed = service.encerrarPendentes(request.getId());

        assertEquals(1, closed);
        assertEquals(RequestOpportunityStatus.ENCERRADA, pending.getStatus());
        assertEquals(RequestOpportunityStatus.ACEITA, accepted.getStatus());
    }

    private ServiceRequest request() {
        ServiceCatalogItem service = new ServiceCatalogItem();
        ServiceRequest request = new ServiceRequest();
        request.setId(42L);
        request.setServico(service);
        request.setEnderecoCidade("Campinas");
        request.setEnderecoUf("SP");
        request.setStatus(ServiceRequestStatus.CRIADA);
        return request;
    }

    private RequestOpportunity opportunity(long id, ServiceRequest request, User provider) {
        RequestOpportunity opportunity = new RequestOpportunity();
        opportunity.setId(id);
        opportunity.setSolicitacao(request);
        opportunity.setPrestador(provider);
        return opportunity;
    }

    private User user(int id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static class FakeCoverageRepository extends ProviderRegionRepository {
        private final List<User> providers;
        private String city;
        private String uf;

        FakeCoverageRepository(List<User> providers) {
            this.providers = providers;
        }

        @Override
        public List<User> buscarPrestadoresCorrespondentes(ServiceCatalogItem service, String city, String uf) {
            this.city = city;
            this.uf = uf;
            return providers;
        }
    }

    private static class FakeOpportunityRepository extends RequestOpportunityRepository {
        private final List<RequestOpportunity> records = new ArrayList<>();

        @Override
        public void persist(RequestOpportunity opportunity) {
            opportunity.setId((long) (records.size() + 1));
            records.add(opportunity);
        }

        @Override
        public RequestOpportunity buscarParaPrestador(Long id, User provider) {
            return records.stream().filter(item -> item.getId().equals(id)
                    && item.getPrestador().getId().equals(provider.getId())).findFirst().orElse(null);
        }

        @Override
        public RequestOpportunity buscarComBloqueio(Long id, User provider) {
            return buscarParaPrestador(id, provider);
        }

        @Override
        public List<RequestOpportunity> list(String query, Object... params) {
            Long requestId = (Long) params[0];
            RequestOpportunityStatus status = (RequestOpportunityStatus) params[1];
            return records.stream().filter(item -> item.getSolicitacao().getId().equals(requestId)
                    && item.getStatus() == status).toList();
        }
    }

    private static class FakeServiceRequestRepository extends ServiceRequestRepository {
        private final ServiceRequest request;

        FakeServiceRequestRepository(ServiceRequest request) {
            this.request = request;
        }

        @Override
        public ServiceRequest buscarComBloqueio(Long id) {
            return request.getId().equals(id) ? request : null;
        }
    }

    private static class FakeConversationRepository extends ConversationRepository {
        private final List<Conversation> records = new ArrayList<>();

        @Override
        public Conversation buscarPorSolicitacaoEPrestador(Long requestId, Integer providerId) {
            return records.stream().filter(item -> item.getSolicitacao().getId().equals(requestId)
                    && item.getPrestador().getId().equals(providerId)).findFirst().orElse(null);
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
}
