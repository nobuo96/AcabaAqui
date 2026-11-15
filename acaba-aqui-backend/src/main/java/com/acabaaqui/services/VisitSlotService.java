package com.acabaaqui.services;

import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.model.Conversation;
import com.acabaaqui.model.User;
import com.acabaaqui.model.VisitSlot;
import com.acabaaqui.model.VisitSlotStatus;
import com.acabaaqui.persistence.ConversationRepository;
import com.acabaaqui.persistence.RequestOpportunityRepository;
import com.acabaaqui.persistence.ServiceRequestRepository;
import com.acabaaqui.persistence.UserRepository;
import com.acabaaqui.persistence.VisitSlotRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.zone.ZoneRules;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class VisitSlotService {

    @Inject
    VisitSlotRepository slotRepository;

    @Inject
    ServiceRequestRepository requestRepository;

    @Inject
    RequestOpportunityRepository opportunityRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ConversationRepository conversationRepository;

    @Inject
    LifecycleStatusService lifecycleStatusService;

    @Transactional
    public void criarDisponibilidades(
            ServiceRequest request,
            List<AvailabilityRequest> availability,
            User actor) {
        if (availability == null || availability.isEmpty()) {
            return;
        }

        Set<LocalDateTime> startTimes = new HashSet<>();
        String requestTimeZone = null;
        for (AvailabilityRequest item : availability) {
            if (item == null || item.inicioLocal() == null) {
                throw new IllegalArgumentException("Informe o início de cada horário.");
            }
            if (item.inicioLocal().getMinute() != 0
                    || item.inicioLocal().getSecond() != 0
                    || item.inicioLocal().getNano() != 0) {
                throw new IllegalArgumentException("Os horários devem começar exatamente na hora.");
            }

            ZoneId zone = validarFuso(item.timeZoneId());
            if (requestTimeZone == null) {
                requestTimeZone = zone.getId();
            } else if (!requestTimeZone.equals(zone.getId())) {
                throw new IllegalArgumentException("Todos os horários da solicitação devem usar o mesmo fuso.");
            }
            ZoneRules rules = zone.getRules();
            if (rules.getValidOffsets(item.inicioLocal()).size() != 1) {
                throw new IllegalArgumentException("O horário não é único ou não existe nesse fuso.");
            }
            if (!item.inicioLocal().isAfter(LocalDateTime.now(zone))) {
                throw new IllegalArgumentException("Os horários devem estar no futuro.");
            }
            if (!startTimes.add(item.inicioLocal())) {
                throw new IllegalArgumentException("Não repita horários na mesma solicitação.");
            }
        }

        for (AvailabilityRequest item : availability) {
            VisitSlot slot = new VisitSlot();
            slot.setSolicitacao(request);
            slot.setInicioLocal(item.inicioLocal());
            slot.setTimeZoneId(requestTimeZone);
            slot.setStatus(VisitSlotStatus.DISPONIVEL);
            slotRepository.persist(slot);
            lifecycleStatusService.registrarTransicao(
                    LifecycleStatusTransitionPolicy.VISITA,
                    slot.getId(),
                    null,
                    VisitSlotStatus.DISPONIVEL.name(),
                    LifecycleStatusTransitionPolicy.CLIENTE,
                    actor);
        }
    }

    @Transactional
    public List<VisitSlot> listar(
            Long requestId,
            Integer userId,
            boolean clientRole,
            boolean providerRole) {
        ServiceRequest request = requestRepository.buscarPorId(requestId);
        if (request == null || !podeAcessar(request, userId, clientRole, providerRole)) {
            return null;
        }

        request = requestRepository.buscarComBloqueio(requestId);
        if (request == null) {
            return null;
        }
        List<VisitSlot> slots = slotRepository.listarPorSolicitacao(requestId);
        slots.forEach(this::expirarSeNecessario);
        return slots;
    }

    @Transactional
    public VisitSlot reservar(Long slotId, Integer providerId) {
        VisitSlot initial = slotRepository.buscarPorId(slotId);
        if (initial == null) {
            return null;
        }
        Long requestId = initial.getSolicitacao().getId();
        ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
        VisitSlot slot = slotRepository.buscarComBloqueio(slotId);
        User provider = userRepository.buscarUsuarioPorId(providerId);
        if (request == null || slot == null || provider == null
                || !requestId.equals(slot.getSolicitacao().getId())
                || !opportunityRepository.temOportunidadeAceita(requestId, providerId)) {
            return null;
        }
        if (request.getStatus() != ServiceRequestStatus.CRIADA) {
            throw new IllegalStateException("A solicitação não está mais disponível.");
        }

        if (expirarSeNecessario(slot)) {
            return slot;
        }
        if ((slot.getStatus() == VisitSlotStatus.RESERVADA
                || slot.getStatus() == VisitSlotStatus.CONFIRMADA)
                && slot.getPrestador() != null
                && slot.getPrestador().getId().equals(providerId)) {
            inicializarPrestador(slot);
            return slot;
        }
        if (slot.getStatus() != VisitSlotStatus.DISPONIVEL) {
            throw new IllegalStateException("Este horário não está disponível.");
        }
        if (!slot.getInicioLocal().isAfter(LocalDateTime.now(ZoneId.of(slot.getTimeZoneId())))) {
            expirarSeNecessario(slot);
            return slot;
        }
        if (!slotRepository.listarAtivasPorPrestador(requestId, providerId).isEmpty()) {
            throw new IllegalStateException("Você já tem uma visita ativa nesta solicitação.");
        }

        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.VISITA,
                slot.getId(),
                slot.getStatus().name(),
                VisitSlotStatus.RESERVADA.name(),
                LifecycleStatusTransitionPolicy.PRESTADOR,
                provider);
        slot.setPrestador(provider);
        slot.setStatus(VisitSlotStatus.RESERVADA);
        return slot;
    }

    @Transactional
    public ConfirmationResult confirmar(Long slotId, Integer clientId) {
        VisitSlot initial = slotRepository.buscarPorId(slotId);
        if (initial == null) {
            return null;
        }
        Long requestId = initial.getSolicitacao().getId();
        ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
        VisitSlot slot = slotRepository.buscarComBloqueio(slotId);
        if (request == null || slot == null || !requestId.equals(slot.getSolicitacao().getId())
                || !request.getUsuario().getId().equals(clientId)) {
            return null;
        }
        if (expirarSeNecessario(slot)) {
            return new ConfirmationResult(slot, null);
        }
        if (request.getStatus() != ServiceRequestStatus.CRIADA) {
            throw new IllegalStateException("A solicitação não está mais disponível.");
        }
        if (slot.getStatus() == VisitSlotStatus.CONFIRMADA) {
            return resultadoConfirmacao(request, slot);
        }
        if (slot.getStatus() != VisitSlotStatus.RESERVADA) {
            throw new IllegalStateException("Não há uma reserva pendente para confirmar.");
        }

        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.VISITA,
                slot.getId(),
                VisitSlotStatus.RESERVADA.name(),
                VisitSlotStatus.CONFIRMADA.name(),
                LifecycleStatusTransitionPolicy.CLIENTE,
                request.getUsuario());
        slot.setStatus(VisitSlotStatus.CONFIRMADA);
        return resultadoConfirmacao(request, slot);
    }

    @Transactional
    public VisitSlot cancelar(
            Long slotId,
            Integer actorId,
            boolean clientRole,
            boolean providerRole) {
        VisitSlot initial = slotRepository.buscarPorId(slotId);
        if (initial == null) {
            return null;
        }
        Long requestId = initial.getSolicitacao().getId();
        ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
        VisitSlot slot = slotRepository.buscarComBloqueio(slotId);
        if (request == null || slot == null || !requestId.equals(slot.getSolicitacao().getId())) {
            return null;
        }

        String actorType = actorTypeForCancellation(request, slot, actorId, clientRole, providerRole);
        if (actorType == null) {
            return null;
        }
        if (expirarSeNecessario(slot)) {
            return slot;
        }
        if (slot.getStatus() != VisitSlotStatus.RESERVADA
                && slot.getStatus() != VisitSlotStatus.CONFIRMADA) {
            throw new IllegalStateException("Não há uma visita ativa para cancelar.");
        }

        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.VISITA,
                slot.getId(),
                slot.getStatus().name(),
                VisitSlotStatus.CANCELADA.name(),
                actorType,
                userRepository.buscarUsuarioPorId(actorId));
        slot.setStatus(VisitSlotStatus.CANCELADA);
        slot.setPrestador(null);

        if (request.getStatus() == ServiceRequestStatus.CRIADA
                && slot.getInicioLocal().isAfter(LocalDateTime.now(ZoneId.of(slot.getTimeZoneId())))) {
            lifecycleStatusService.registrarTransicao(
                    LifecycleStatusTransitionPolicy.VISITA,
                    slot.getId(),
                    VisitSlotStatus.CANCELADA.name(),
                    VisitSlotStatus.DISPONIVEL.name(),
                    LifecycleStatusTransitionPolicy.SISTEMA,
                    null);
            slot.setStatus(VisitSlotStatus.DISPONIVEL);
        }
        return slot;
    }

    @Transactional
    public void cancelarTodosPorSolicitacao(ServiceRequest request, User actor) {
        cancelarTodosPorSolicitacao(request, actor, LifecycleStatusTransitionPolicy.CLIENTE);
    }

    @Transactional
    public void cancelarTodosPorSolicitacao(ServiceRequest request, User actor, String actorType) {
        List<VisitSlot> slots = slotRepository.listarPorSolicitacao(request.getId());
        for (VisitSlot slot : slots) {
            if (expirarSeNecessario(slot)) {
                continue;
            }
            if (slot.getStatus() == VisitSlotStatus.DISPONIVEL
                    || slot.getStatus() == VisitSlotStatus.RESERVADA
                    || slot.getStatus() == VisitSlotStatus.CONFIRMADA) {
                lifecycleStatusService.registrarTransicao(
                        LifecycleStatusTransitionPolicy.VISITA,
                        slot.getId(),
                        slot.getStatus().name(),
                        VisitSlotStatus.CANCELADA.name(),
                        actorType,
                        actor);
                slot.setStatus(VisitSlotStatus.CANCELADA);
                slot.setPrestador(null);
            }
        }
    }

    private boolean podeAcessar(
            ServiceRequest request,
            Integer userId,
            boolean clientRole,
            boolean providerRole) {
        if (userId == null) {
            return false;
        }
        if (clientRole && request.getUsuario().getId().equals(userId)) {
            return true;
        }
        return providerRole && opportunityRepository.temOportunidadeAceita(request.getId(), userId);
    }

    private String actorTypeForCancellation(
            ServiceRequest request,
            VisitSlot slot,
            Integer actorId,
            boolean clientRole,
            boolean providerRole) {
        if (actorId == null) {
            return null;
        }
        if (clientRole && request.getUsuario().getId().equals(actorId)) {
            return LifecycleStatusTransitionPolicy.CLIENTE;
        }
        if (providerRole && slot.getPrestador() != null
                && slot.getPrestador().getId().equals(actorId)) {
            return LifecycleStatusTransitionPolicy.PRESTADOR;
        }
        return null;
    }

    private boolean expirarSeNecessario(VisitSlot slot) {
        VisitSlotStatus status = slot.getStatus();
        if (status != VisitSlotStatus.DISPONIVEL
                && status != VisitSlotStatus.RESERVADA
                && status != VisitSlotStatus.CONFIRMADA) {
            return status == VisitSlotStatus.EXPIRADA;
        }

        ZoneId zone = ZoneId.of(slot.getTimeZoneId());
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime expiration = status == VisitSlotStatus.DISPONIVEL
                ? slot.getInicioLocal()
                : slot.getInicioLocal().plusHours(1);
        if (expiration.isAfter(now)) {
            return false;
        }

        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.VISITA,
                slot.getId(),
                status.name(),
                VisitSlotStatus.EXPIRADA.name(),
                LifecycleStatusTransitionPolicy.SISTEMA,
                null);
        slot.setStatus(VisitSlotStatus.EXPIRADA);
        if (status == VisitSlotStatus.CONFIRMADA) {
            slot.setPrestadorConfirmado(slot.getPrestador());
        }
        slot.setPrestador(null);
        return true;
    }

    private ZoneId validarFuso(String timeZoneId) {
        if (timeZoneId == null || !ZoneId.getAvailableZoneIds().contains(timeZoneId)) {
            throw new IllegalArgumentException("Informe um timeZoneId IANA válido para o endereço.");
        }
        return ZoneId.of(timeZoneId);
    }

    private void inicializarPrestador(VisitSlot slot) {
        if (slot.getPrestador() != null) {
            slot.getPrestador().getNome();
        }
    }

    private ConfirmationResult resultadoConfirmacao(ServiceRequest request, VisitSlot slot) {
        User provider = slot.getPrestador();
        if (provider == null) {
            throw new IllegalStateException("A reserva não tem um prestador associado.");
        }
        Conversation conversation = conversationRepository.buscarPorSolicitacaoEPrestador(
                request.getId(), provider.getId());
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setSolicitacao(request);
            conversation.setCliente(request.getUsuario());
            conversation.setPrestador(provider);
            conversationRepository.persist(conversation);
        }
        inicializarPrestador(slot);
        return new ConfirmationResult(slot, conversation.getId());
    }

    public record ConfirmationResult(VisitSlot slot, Long conversationId) {
    }

    public record AvailabilityRequest(LocalDateTime inicioLocal, String timeZoneId) {
    }
}
