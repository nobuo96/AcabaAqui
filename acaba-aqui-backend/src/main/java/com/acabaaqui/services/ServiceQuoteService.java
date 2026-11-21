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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@ApplicationScoped
public class ServiceQuoteService {

    private static final long VALIDITY_DAYS = 7;

    @Inject
    ServiceQuoteRepository quoteRepository;

    @Inject
    ServiceRequestRepository requestRepository;

    @Inject
    RequestOpportunityRepository opportunityRepository;

    @Inject
    VisitSlotRepository visitSlotRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    InAppNotificationService notificationService;

    @Inject
    VisitSlotService visitSlotService;

    @Inject
    LifecycleStatusService lifecycleStatusService;

    @Transactional
    public ServiceQuote enviar(Integer providerId, Long requestId, QuoteSubmission submission) {
        QuoteSubmission validated = validar(submission);
        ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
        User provider = userRepository.buscarUsuarioPorId(providerId);
        if (request == null || provider == null) {
            return null;
        }
        if (request.getStatus() != ServiceRequestStatus.CRIADA
                || !opportunityRepository.temOportunidadeAceita(requestId, providerId)) {
            throw new IllegalStateException("Você não pode enviar orçamento para esta solicitação.");
        }
        if (!temVisitaConcluida(requestId, providerId)) {
            throw new IllegalStateException("O orçamento só pode ser enviado após a visita confirmada.");
        }

        List<ServiceQuote> quotes = quoteRepository.listarPorSolicitacaoComBloqueio(requestId);
        if (quotes.stream().anyMatch(quote -> quote.getStatus() == ServiceQuoteStatus.APROVADO)) {
            throw new IllegalStateException("A solicitação já tem um orçamento aprovado.");
        }
        if (quotes.stream().anyMatch(quote -> quote.getPrestador().getId().equals(providerId))) {
            throw new IllegalStateException("Você já enviou orçamento para esta solicitação.");
        }

        ServiceQuote quote = new ServiceQuote();
        quote.setSolicitacao(request);
        quote.setPrestador(provider);
        quote.setDescricao(validated.descricao());
        quote.setValorTotal(validated.valorTotal());
        quote.setGarantiaDias(validated.garantiaDias());
        quote.setExpiraEm(LocalDateTime.now().plusDays(VALIDITY_DAYS));
        quoteRepository.persist(quote);
        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.ORCAMENTO,
                quote.getId(),
                null,
                ServiceQuoteStatus.ENVIADO.name(),
                LifecycleStatusTransitionPolicy.PRESTADOR,
                provider);
        notificationService.criar(
                request.getUsuario(),
                request,
                quote,
                "ORCAMENTO_ENVIADO",
                "Um prestador enviou um orçamento para sua solicitação.");
        return quote;
    }

    @Transactional
    public List<ServiceQuote> listar(
            Long requestId,
            Integer userId,
            boolean clientRole,
            boolean providerRole) {
        ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
        if (request == null) {
            return null;
        }
        boolean clientAccess = clientRole && request.getUsuario().getId().equals(userId);
        boolean providerAccess = providerRole
                && opportunityRepository.temOportunidadeAceita(requestId, userId);
        if (!clientAccess && !providerAccess) {
            return null;
        }

        List<ServiceQuote> quotes = quoteRepository.listarPorSolicitacaoComBloqueio(requestId);
        expirarVencidos(quotes, LocalDateTime.now());
        if (clientAccess) {
            return quotes;
        }
        return quotes.stream().filter(quote -> quote.getPrestador().getId().equals(userId)).toList();
    }

    @Transactional
    public ServiceQuote aprovar(Long quoteId, Integer clientId) {
        return decidir(quoteId, clientId, true);
    }

    @Transactional
    public ServiceQuote recusar(Long quoteId, Integer clientId) {
        return decidir(quoteId, clientId, false);
    }

    @Transactional
    public void cancelarTodosPorSolicitacao(ServiceRequest request, User actor, String actorType) {
        List<ServiceQuote> quotes = quoteRepository.listarPorSolicitacaoComBloqueio(request.getId());
        for (ServiceQuote quote : quotes) {
            if (quote.getStatus() != ServiceQuoteStatus.ENVIADO
                    && quote.getStatus() != ServiceQuoteStatus.APROVADO) {
                continue;
            }
            alterarStatus(quote, ServiceQuoteStatus.CANCELADO, actorType, actor);
            notificationService.criar(
                    quote.getPrestador(),
                    request,
                    quote,
                    "ORCAMENTO_CANCELADO",
                    "A solicitação foi encerrada e o orçamento foi cancelado.");
        }
    }

    @Transactional
    public int expirarVencidos(Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<ServiceQuoteRepository.ExpirationCandidate> candidates = quoteRepository.listarExpiradosAte(now);
        int expired = 0;
        for (ServiceQuoteRepository.ExpirationCandidate candidate : candidates) {
            ServiceRequest request = requestRepository.buscarComBloqueio(candidate.requestId());
            if (request == null) {
                continue;
            }
            List<ServiceQuote> quotes =
                    quoteRepository.listarPorSolicitacaoComBloqueio(candidate.requestId());
            ServiceQuote quote = quotes.stream()
                    .filter(item -> item.getId().equals(candidate.quoteId()))
                    .findFirst()
                    .orElse(null);
            if (quote != null && expirarSeNecessario(quote, now)) {
                expired++;
            }
        }
        return expired;
    }

    private ServiceQuote decidir(Long quoteId, Integer clientId, boolean approve) {
        Long requestId = quoteRepository.buscarSolicitacaoId(quoteId);
        if (requestId == null) {
            return null;
        }
        ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
        if (request == null || !request.getUsuario().getId().equals(clientId)) {
            return null;
        }
        List<ServiceQuote> quotes = quoteRepository.listarPorSolicitacaoComBloqueio(requestId);
        LocalDateTime now = LocalDateTime.now();
        expirarVencidos(quotes, now);
        ServiceQuote selected = quotes.stream()
                .filter(quote -> quote.getId().equals(quoteId))
                .findFirst()
                .orElse(null);
        if (selected == null) {
            return null;
        }
        if ((approve && selected.getStatus() == ServiceQuoteStatus.APROVADO)
                || (!approve && selected.getStatus() == ServiceQuoteStatus.RECUSADO)) {
            return selected;
        }
        if (selected.getStatus() == ServiceQuoteStatus.EXPIRADO) {
            return selected;
        }
        if (request.getStatus() != ServiceRequestStatus.CRIADA
                || selected.getStatus() != ServiceQuoteStatus.ENVIADO) {
            throw new IllegalStateException("Este orçamento não está mais disponível para decisão.");
        }

        if (!approve) {
            alterarStatus(
                    selected,
                    ServiceQuoteStatus.RECUSADO,
                    LifecycleStatusTransitionPolicy.CLIENTE,
                    request.getUsuario());
            notificationService.criar(
                    selected.getPrestador(),
                    request,
                    selected,
                    "ORCAMENTO_RECUSADO",
                    "O cliente recusou seu orçamento.");
            return selected;
        }

        if (quotes.stream().anyMatch(quote -> quote.getStatus() == ServiceQuoteStatus.APROVADO)) {
            throw new IllegalStateException("A solicitação já tem um orçamento aprovado.");
        }
        alterarStatus(
                selected,
                ServiceQuoteStatus.APROVADO,
                LifecycleStatusTransitionPolicy.CLIENTE,
                request.getUsuario());
        notificationService.criar(
                selected.getPrestador(),
                request,
                selected,
                "ORCAMENTO_APROVADO",
                "Seu orçamento foi aprovado pelo cliente.");

        for (ServiceQuote other : quotes) {
            if (other == selected || other.getStatus() != ServiceQuoteStatus.ENVIADO) {
                continue;
            }
            alterarStatus(
                    other,
                    ServiceQuoteStatus.CANCELADO,
                    LifecycleStatusTransitionPolicy.CLIENTE,
                    request.getUsuario());
            notificationService.criar(
                    other.getPrestador(),
                    request,
                    other,
                    "ORCAMENTO_CANCELADO",
                    "O cliente aprovou outro orçamento; o seu foi cancelado.");
        }
        visitSlotService.cancelarTodosPorSolicitacao(
                request, request.getUsuario(), LifecycleStatusTransitionPolicy.CLIENTE);
        return selected;
    }

    private void expirarVencidos(List<ServiceQuote> quotes, LocalDateTime now) {
        quotes.forEach(quote -> expirarSeNecessario(quote, now));
    }

    private boolean expirarSeNecessario(ServiceQuote quote, LocalDateTime now) {
        if (quote.getStatus() != ServiceQuoteStatus.ENVIADO || quote.getExpiraEm().isAfter(now)) {
            return false;
        }
        alterarStatus(
                quote,
                ServiceQuoteStatus.EXPIRADO,
                LifecycleStatusTransitionPolicy.SISTEMA,
                null);
        notificationService.criar(
                quote.getSolicitacao().getUsuario(),
                quote.getSolicitacao(),
                quote,
                "ORCAMENTO_EXPIRADO",
                "Um orçamento para sua solicitação expirou.");
        notificationService.criar(
                quote.getPrestador(),
                quote.getSolicitacao(),
                quote,
                "ORCAMENTO_EXPIRADO",
                "Seu orçamento expirou.");
        return true;
    }

    private void alterarStatus(ServiceQuote quote, ServiceQuoteStatus status, String actorType, User actor) {
        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.ORCAMENTO,
                quote.getId(),
                quote.getStatus().name(),
                status.name(),
                actorType,
                actor);
        quote.setStatus(status);
    }

    private boolean temVisitaConcluida(Long requestId, Integer providerId) {
        for (VisitSlot slot : visitSlotRepository.listarPorSolicitacao(requestId)) {
            User visitProvider = slot.getStatus() == VisitSlotStatus.EXPIRADA
                    ? slot.getPrestadorConfirmado()
                    : slot.getPrestador();
            if ((slot.getStatus() != VisitSlotStatus.CONFIRMADA
                    && slot.getStatus() != VisitSlotStatus.EXPIRADA)
                    || visitProvider == null
                    || !visitProvider.getId().equals(providerId)) {
                continue;
            }
            LocalDateTime visitEnd = slot.getInicioLocal().plusHours(1);
            if (!visitEnd.isAfter(LocalDateTime.now(ZoneId.of(slot.getTimeZoneId())))) {
                return true;
            }
        }
        return false;
    }

    private QuoteSubmission validar(QuoteSubmission submission) {
        if (submission == null || submission.descricao() == null || submission.valorTotal() == null) {
            throw new IllegalArgumentException("Informe a descrição, o valor total e a duração da garantia.");
        }
        String description = submission.descricao().trim();
        BigDecimal value = submission.valorTotal();
        int warrantyDays = submission.garantiaDias();
        if (description.isEmpty() || description.length() > 2000) {
            throw new IllegalArgumentException("A descrição deve ter entre 1 e 2000 caracteres.");
        }
        if (value.signum() <= 0
                || value.stripTrailingZeros().scale() > 2
                || value.setScale(2).precision() > 12) {
            throw new IllegalArgumentException("Informe um valor positivo com até 2 casas decimais.");
        }
        if (warrantyDays <= 0) {
            throw new IllegalArgumentException("Informe a duração da garantia em dias inteiros positivos.");
        }
        try {
            if (LocalDateTime.now().plusDays(warrantyDays).getYear() > 9999) {
                throw new IllegalArgumentException("A duração da garantia excede o limite suportado.");
            }
        } catch (java.time.DateTimeException exception) {
            throw new IllegalArgumentException("A duração da garantia excede o limite suportado.");
        }
        return new QuoteSubmission(description, value.setScale(2), warrantyDays);
    }

    public record QuoteSubmission(String descricao, BigDecimal valorTotal, int garantiaDias) { }
}
