package com.acabaaqui.services;

import com.acabaaqui.model.RequestOpportunity;
import com.acabaaqui.model.RequestOpportunityStatus;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.ProviderRegionRepository;
import com.acabaaqui.persistence.RequestOpportunityRepository;
import com.acabaaqui.persistence.ServiceRequestRepository;
import com.acabaaqui.persistence.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class RequestOpportunityService {

    @Inject
    ProviderRegionRepository areaRepository;

    @Inject
    RequestOpportunityRepository opportunityRepository;

    @Inject
    ServiceRequestRepository requestRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    LifecycleStatusService lifecycleStatusService;

    @Inject
    ServiceRequestClosureService closureService;

    public List<RequestOpportunity> listar(Integer providerId) {
        User provider = userRepository.buscarUsuarioPorId(providerId);
        return provider == null ? null : opportunityRepository.listarPorPrestador(provider);
    }

    @Transactional
    public void notificarCorrespondentes(ServiceRequest request) {
        List<User> providers = areaRepository.buscarPrestadoresCorrespondentes(
                request.getServico(), request.getEnderecoCidade(), request.getEnderecoUf());
        for (User provider : providers) {
            RequestOpportunity opportunity = new RequestOpportunity();
            opportunity.setSolicitacao(request);
            opportunity.setPrestador(provider);
            opportunityRepository.persist(opportunity);
            lifecycleStatusService.registrarTransicao(
                    LifecycleStatusTransitionPolicy.OPORTUNIDADE,
                    opportunity.getId(),
                    null,
                    RequestOpportunityStatus.PENDENTE.name(),
                    LifecycleStatusTransitionPolicy.SISTEMA,
                    null);
        }
    }

    @Transactional
    public int marcarLidas(Integer providerId) {
        List<RequestOpportunity> unread = opportunityRepository.list(
                "prestador.id = ?1 and lida = false", providerId);
        unread.forEach(opportunity -> opportunity.setLida(true));
        return unread.size();
    }

    @Transactional
    public RequestOpportunity aceitar(Integer providerId, Long opportunityId) {
        RequestOpportunity initial = opportunityRepository.buscarParaPrestador(
                opportunityId, userRepository.buscarUsuarioPorId(providerId));
        if (initial == null) {
            return null;
        }

        ServiceRequest request = requestRepository.buscarComBloqueio(initial.getSolicitacao().getId());
        if (request == null) {
            return null;
        }
        User provider = userRepository.buscarUsuarioPorId(providerId);
        RequestOpportunity opportunity = opportunityRepository.buscarComBloqueio(opportunityId, provider);
        if (opportunity == null) {
            return null;
        }
        if (opportunity.getStatus() == RequestOpportunityStatus.ACEITA) {
            return opportunity;
        }
        validarPendente(request, opportunity);

        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.OPORTUNIDADE,
                opportunity.getId(),
                opportunity.getStatus().name(),
                RequestOpportunityStatus.ACEITA.name(),
                LifecycleStatusTransitionPolicy.PRESTADOR,
                provider);
        opportunity.setStatus(RequestOpportunityStatus.ACEITA);
        opportunity.setLida(true);
        return opportunity;
    }

    @Transactional
    public RequestOpportunity recusar(Integer providerId, Long opportunityId) {
        RequestOpportunity initial = opportunityRepository.buscarParaPrestador(
                opportunityId, userRepository.buscarUsuarioPorId(providerId));
        if (initial == null) {
            return null;
        }
        ServiceRequest request = requestRepository.buscarComBloqueio(initial.getSolicitacao().getId());
        if (request == null) {
            return null;
        }
        RequestOpportunity opportunity = opportunityRepository.buscarComBloqueio(
                opportunityId, userRepository.buscarUsuarioPorId(providerId));
        if (opportunity == null) {
            return null;
        }
        validarPendente(request, opportunity);
        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.OPORTUNIDADE,
                opportunity.getId(),
                opportunity.getStatus().name(),
                RequestOpportunityStatus.RECUSADA.name(),
                LifecycleStatusTransitionPolicy.PRESTADOR,
                opportunity.getPrestador());
        opportunity.setStatus(RequestOpportunityStatus.RECUSADA);
        opportunity.setLida(true);
        closureService.encerrarSeTodasRecusadas(request);
        return opportunity;
    }

    @Transactional
    public int encerrarPendentes(Long requestId) {
        return encerrarPendentes(requestId, null, LifecycleStatusTransitionPolicy.SISTEMA);
    }

    @Transactional
    public int encerrarPendentes(Long requestId, User actor, String actorType) {
        List<RequestOpportunity> pending = opportunityRepository.list(
                "solicitacao.id = ?1 and status = ?2", requestId, RequestOpportunityStatus.PENDENTE);
        pending.forEach(opportunity -> {
            lifecycleStatusService.registrarTransicao(
                    LifecycleStatusTransitionPolicy.OPORTUNIDADE,
                    opportunity.getId(),
                    opportunity.getStatus().name(),
                    RequestOpportunityStatus.ENCERRADA.name(),
                    actorType,
                    actor);
            opportunity.setStatus(RequestOpportunityStatus.ENCERRADA);
        });
        return pending.size();
    }

    private void validarPendente(ServiceRequest request, RequestOpportunity opportunity) {
        if (request.getStatus() != ServiceRequestStatus.CRIADA
                || opportunity.getStatus() != RequestOpportunityStatus.PENDENTE) {
            throw new IllegalStateException("Esta oportunidade não está mais disponível.");
        }
    }

}
