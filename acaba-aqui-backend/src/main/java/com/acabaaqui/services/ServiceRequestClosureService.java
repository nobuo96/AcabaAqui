package com.acabaaqui.services;

import com.acabaaqui.model.RequestOpportunity;
import com.acabaaqui.model.RequestOpportunityStatus;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.RequestOpportunityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class ServiceRequestClosureService {

    @Inject
    RequestOpportunityRepository opportunityRepository;

    @Inject
    LifecycleStatusService lifecycleStatusService;

    @Inject
    VisitSlotService visitSlotService;

    @Inject
    ServiceQuoteService quoteService;

    @Transactional
    public boolean encerrarSeTodasRecusadas(ServiceRequest request) {
        if (request == null || request.getStatus() != ServiceRequestStatus.CRIADA) {
            return false;
        }
        List<RequestOpportunity> opportunities =
                opportunityRepository.listarPorSolicitacaoComBloqueio(request.getId());
        if (opportunities.isEmpty()
                || opportunities.stream().anyMatch(item -> item.getStatus() != RequestOpportunityStatus.RECUSADA)) {
            return false;
        }
        return encerrar(request, null, LifecycleStatusTransitionPolicy.SISTEMA, opportunities);
    }

    @Transactional
    public boolean encerrarSeSemAceite(ServiceRequest request) {
        if (request == null || request.getStatus() != ServiceRequestStatus.CRIADA) {
            return false;
        }
        List<RequestOpportunity> opportunities =
                opportunityRepository.listarPorSolicitacaoComBloqueio(request.getId());
        if (opportunities.stream().anyMatch(item -> item.getStatus() == RequestOpportunityStatus.ACEITA)) {
            return false;
        }
        return encerrar(request, null, LifecycleStatusTransitionPolicy.SISTEMA, opportunities);
    }

    @Transactional
    public boolean encerrar(ServiceRequest request, User actor, String actorType) {
        if (request == null || request.getStatus() != ServiceRequestStatus.CRIADA) {
            return false;
        }
        List<RequestOpportunity> opportunities =
                opportunityRepository.listarPorSolicitacaoComBloqueio(request.getId());
        return encerrar(request, actor, actorType, opportunities);
    }

    private boolean encerrar(
            ServiceRequest request,
            User actor,
            String actorType,
            List<RequestOpportunity> opportunities) {
        if (request.getStatus() != ServiceRequestStatus.CRIADA) {
            return false;
        }
        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.SOLICITACAO,
                request.getId(),
                request.getStatus().name(),
                ServiceRequestStatus.ENCERRADA.name(),
                actorType,
                actor);
        request.setStatus(ServiceRequestStatus.ENCERRADA);
        quoteService.cancelarTodosPorSolicitacao(request, actor, actorType);
        visitSlotService.cancelarTodosPorSolicitacao(request, actor, actorType);
        for (RequestOpportunity opportunity : opportunities) {
            if (opportunity.getStatus() != RequestOpportunityStatus.PENDENTE) {
                continue;
            }
            lifecycleStatusService.registrarTransicao(
                    LifecycleStatusTransitionPolicy.OPORTUNIDADE,
                    opportunity.getId(),
                    opportunity.getStatus().name(),
                    RequestOpportunityStatus.ENCERRADA.name(),
                    actorType,
                    actor);
            opportunity.setStatus(RequestOpportunityStatus.ENCERRADA);
        }
        return true;
    }
}
