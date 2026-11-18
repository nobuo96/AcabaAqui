package com.acabaaqui.services;

import com.acabaaqui.model.ServiceCancellation;
import com.acabaaqui.model.ServiceExecution;
import com.acabaaqui.model.ServiceExecutionStatus;
import com.acabaaqui.model.PaymentStatus;
import com.acabaaqui.model.ServicePayment;
import com.acabaaqui.model.ServiceQuote;
import com.acabaaqui.model.ServiceQuoteStatus;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.LifecycleStatusHistoryRepository;
import com.acabaaqui.persistence.ServiceCancellationRepository;
import com.acabaaqui.persistence.ServiceExecutionRepository;
import com.acabaaqui.persistence.ServicePaymentRepository;
import com.acabaaqui.persistence.ServiceQuoteRepository;
import com.acabaaqui.persistence.ServiceRequestRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ServiceExecutionService {

    private static final long APPROVAL_WINDOW_DAYS = 7;

    @Inject
    ServiceExecutionRepository executionRepository;

    @Inject
    ServicePaymentRepository paymentRepository;

    @Inject
    ServiceCancellationRepository cancellationRepository;

    @Inject
    ServiceQuoteRepository quoteRepository;

    @Inject
    ServiceRequestRepository requestRepository;

    @Inject
    LifecycleStatusHistoryRepository historyRepository;

    @Inject
    LifecycleStatusService lifecycleStatusService;

    @Inject
    InAppNotificationService notificationService;

    @Inject
    ServicePayoutService payoutService;

    @Transactional
    public List<ExecutionResponse> listar(Integer userId, boolean provider) {
        List<ServiceExecution> executions = provider
                ? executionRepository.listarDoPrestador(userId)
                : executionRepository.listarDoCliente(userId);
        return executions.stream().map(execution -> toResponse(execution, false)).toList();
    }

    @Transactional
    public ExecutionResponse buscar(Integer userId, boolean provider, Long quoteId) {
        ServiceExecution execution = executionRepository.buscarDetalhe(quoteId);
        if (execution == null || !participa(execution.getOrcamento(), userId, provider)) {
            throw notFound();
        }
        return toResponse(execution, true);
    }

    @Transactional
    public ExecutionResponse iniciar(Integer providerId, Long quoteId) {
        LockedExecution locked = bloquear(quoteId);
        validarPrestador(locked.quote(), providerId);
        ServiceExecution execution = locked.execution();
        ServiceCancellation cancellation =
                cancellationRepository.buscarPorPagamentoComBloqueio(locked.payment().getId());
        ServiceExecutionPolicy.validarInicio(
                execution.getStatus(), locked.payment().getStatus(), cancellation != null);

        LocalDateTime now = LocalDateTime.now();
        transicionar(execution, ServiceExecutionStatus.EM_ANDAMENTO,
                LifecycleStatusTransitionPolicy.PRESTADOR, locked.quote().getPrestador());
        execution.setIniciadoEm(now);
        notificationService.criar(
                locked.quote().getSolicitacao().getUsuario(),
                locked.request(),
                locked.quote(),
                "SERVICO_INICIADO",
                "O prestador iniciou a execução do serviço.");
        return toResponse(execution, true);
    }

    @Transactional
    public ExecutionResponse concluir(Integer providerId, Long quoteId) {
        LockedExecution locked = bloquear(quoteId);
        validarPrestador(locked.quote(), providerId);
        ServiceExecution execution = locked.execution();
        ServiceExecutionPolicy.validarConclusao(execution.getStatus());

        LocalDateTime now = LocalDateTime.now();
        transicionar(execution, ServiceExecutionStatus.CONCLUIDA_PELO_PRESTADOR,
                LifecycleStatusTransitionPolicy.PRESTADOR, locked.quote().getPrestador());
        execution.setConcluidoEm(now);
        execution.setAprovarAte(now.plusDays(APPROVAL_WINDOW_DAYS));
        notificationService.criar(
                locked.request().getUsuario(),
                locked.request(),
                locked.quote(),
                "SERVICO_CONCLUIDO",
                "O prestador concluiu o serviço. Você tem 7 dias corridos para revisar e aprovar.");
        return toResponse(execution, true);
    }

    @Transactional
    public ExecutionResponse aprovar(Integer clientId, Long quoteId) {
        LockedExecution locked = bloquear(quoteId);
        validarCliente(locked.request(), clientId);
        ServiceExecution execution = locked.execution();
        if (execution.getStatus() == ServiceExecutionStatus.APROVADA) {
            return toResponse(execution, true);
        }
        if (locked.payment().getStatus() != PaymentStatus.PAGO
                || historyRepository.existeStatus(
                        LifecycleStatusTransitionPolicy.DISPUTA, quoteId, "ABERTA")) {
            throw conflict("Este serviço não pode ser aprovado enquanto o pagamento estiver em disputa ou reembolso.");
        }
        LocalDateTime now = LocalDateTime.now();
        ServiceExecutionPolicy.validarAprovacao(execution.getStatus(), execution.getAprovarAte(), now);
        transicionar(execution, ServiceExecutionStatus.APROVADA,
                LifecycleStatusTransitionPolicy.CLIENTE, locked.request().getUsuario());
        execution.setAprovadoEm(now);
        if (!payoutService.enfileirarAposAprovacao(quoteId)) {
            throw conflict("Não foi possível criar a pendência de repasse deste serviço.");
        }
        notificationService.criar(
                locked.quote().getPrestador(),
                locked.request(),
                locked.quote(),
                "SERVICO_APROVADO",
                "O cliente aprovou a conclusão do serviço.");
        return toResponse(execution, true);
    }

    @Transactional
    public void criarAguardandoSeAusente(ServiceQuote quote) {
        if (executionRepository.findById(quote.getId()) != null) {
            return;
        }
        String currentStatus = historyRepository.buscarUltimoStatus(
                LifecycleStatusTransitionPolicy.EXECUCAO, quote.getId());
        if (ServiceExecutionStatus.CANCELADA.name().equals(currentStatus)) {
            return;
        }
        ServiceExecution execution = new ServiceExecution();
        execution.setOrcamento(quote);
        execution.setStatus(ServiceExecutionStatus.AGUARDANDO_INICIO);
        executionRepository.persist(execution);
        executionRepository.flush();
        if (currentStatus == null) {
            lifecycleStatusService.registrarTransicao(
                    LifecycleStatusTransitionPolicy.EXECUCAO,
                    quote.getId(),
                    null,
                    ServiceExecutionStatus.AGUARDANDO_INICIO.name(),
                    LifecycleStatusTransitionPolicy.SISTEMA,
                    null);
        }
    }

    @Transactional
    public void cancelarAposReembolso(Long quoteId, User actor) {
        ServiceExecution execution = executionRepository.buscarPorOrcamentoComBloqueio(quoteId);
        if (execution == null) {
            return;
        }
        if (execution.getStatus() == ServiceExecutionStatus.CANCELADA) {
            return;
        }
        if (execution.getStatus() != ServiceExecutionStatus.AGUARDANDO_INICIO) {
            throw new IllegalStateException("Um serviço iniciado não pode ser cancelado.");
        }
        transicionar(execution, ServiceExecutionStatus.CANCELADA,
                LifecycleStatusTransitionPolicy.CLIENTE, actor);
    }

    private LockedExecution bloquear(Long quoteId) {
        Long requestId = quoteRepository.buscarSolicitacaoId(quoteId);
        if (requestId == null) {
            throw notFound();
        }
        ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
        ServiceQuote quote = quoteRepository.findById(quoteId);
        if (request == null || quote == null || quote.getStatus() != ServiceQuoteStatus.APROVADO) {
            throw notFound();
        }

        ServicePayment payment = paymentRepository.buscarPorOrcamentoComBloqueio(quoteId);
        if (payment == null) {
            throw conflict("O pagamento deste serviço não foi encontrado.");
        }
        ServiceExecution execution = executionRepository.buscarPorOrcamentoComBloqueio(quoteId);
        if (execution == null) {
            throw conflict("A execução deste serviço não está disponível.");
        }
        return new LockedExecution(request, quote, payment, execution);
    }

    private void transicionar(
            ServiceExecution execution,
            ServiceExecutionStatus next,
            String actorType,
            User actor) {
        ServiceExecutionStatus previous = execution.getStatus();
        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.EXECUCAO,
                execution.getOrcamento().getId(),
                previous.name(),
                next.name(),
                actorType,
                actor);
        execution.setStatus(next);
    }

    private ExecutionResponse toResponse(ServiceExecution execution, boolean includeHistory) {
        ServiceQuote quote = execution.getOrcamento();
        ServiceRequest request = quote.getSolicitacao();
        ServicePayment payment = paymentRepository.buscarPorOrcamento(quote.getId());
        List<HistoryResponse> history = includeHistory
                ? historyRepository.listarPorEntidade(
                        LifecycleStatusTransitionPolicy.EXECUCAO, quote.getId()).stream()
                .map(item -> new HistoryResponse(
                        item.getPreviousStatus(),
                        item.getNewStatus(),
                        item.getActorType(),
                        item.getOccurredAt()))
                .toList()
                : List.of();
        return new ExecutionResponse(
                quote.getId(),
                request.getId(),
                request.getServico().getNome(),
                quote.getDescricao(),
                quote.getValorTotal(),
                request.getUsuario().getId(),
                request.getUsuario().getNome(),
                quote.getPrestador().getId(),
                quote.getPrestador().getNome(),
                execution.getStatus().name(),
                payment == null ? null : payment.getStatus().name(),
                execution.getIniciadoEm(),
                execution.getConcluidoEm(),
                execution.getAprovarAte(),
                execution.getAprovadoEm(),
                history);
    }

    private boolean participa(ServiceQuote quote, Integer userId, boolean provider) {
        Integer participantId = provider
                ? quote.getPrestador().getId()
                : quote.getSolicitacao().getUsuario().getId();
        return Objects.equals(participantId, userId);
    }

    private void validarPrestador(ServiceQuote quote, Integer providerId) {
        if (!Objects.equals(quote.getPrestador().getId(), providerId)) {
            throw notFound();
        }
    }

    private void validarCliente(ServiceRequest request, Integer clientId) {
        if (!Objects.equals(request.getUsuario().getId(), clientId)) {
            throw notFound();
        }
    }

    private WebApplicationException notFound() {
        return new WebApplicationException("Serviço não encontrado.", Response.Status.NOT_FOUND);
    }

    private WebApplicationException conflict(String message) {
        return new WebApplicationException(message, Response.Status.CONFLICT);
    }

    private record LockedExecution(
            ServiceRequest request,
            ServiceQuote quote,
            ServicePayment payment,
            ServiceExecution execution) { }

    public record ExecutionResponse(
            Long quoteId,
            Long requestId,
            String serviceName,
            String description,
            java.math.BigDecimal amount,
            Integer clientId,
            String clientName,
            Integer providerId,
            String providerName,
            String status,
            String paymentStatus,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            LocalDateTime approveBy,
            LocalDateTime approvedAt,
            List<HistoryResponse> history) { }

    public record HistoryResponse(
            String previousStatus,
            String status,
            String actorType,
            LocalDateTime occurredAt) { }
}
