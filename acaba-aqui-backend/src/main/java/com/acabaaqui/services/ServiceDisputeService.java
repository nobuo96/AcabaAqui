package com.acabaaqui.services;

import com.acabaaqui.model.PaymentStatus;
import com.acabaaqui.model.ServiceDispute;
import com.acabaaqui.model.ServiceDisputeDecision;
import com.acabaaqui.model.ServiceDisputeEvidence;
import com.acabaaqui.model.ServiceDisputeStatus;
import com.acabaaqui.model.ServiceExecution;
import com.acabaaqui.model.ServiceExecutionStatus;
import com.acabaaqui.model.ServicePayment;
import com.acabaaqui.model.ServiceQuote;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.ServiceDisputeRepository;
import com.acabaaqui.persistence.ServiceExecutionRepository;
import com.acabaaqui.persistence.ServicePaymentRepository;
import com.acabaaqui.persistence.ServiceQuoteRepository;
import com.acabaaqui.persistence.ServiceRequestRepository;
import com.acabaaqui.persistence.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ServiceDisputeService {

    @Inject
    ServiceDisputeRepository disputeRepository;

    @Inject
    ServiceExecutionRepository executionRepository;

    @Inject
    ServicePaymentRepository paymentRepository;

    @Inject
    ServiceQuoteRepository quoteRepository;

    @Inject
    ServiceRequestRepository requestRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    LifecycleStatusService lifecycleStatusService;

    @Inject
    InAppNotificationService notificationService;

    @Inject
    ServicePaymentService paymentService;

    @Inject
    ServicePayoutService payoutService;

    @Transactional
    public List<DisputeResponse> listarParaAdministrador() {
        return disputeRepository.listarTodos().stream()
                .map(dispute -> toResponse(dispute, false))
                .toList();
    }

    @Transactional
    public DisputeResponse buscar(Long disputeId, Integer userId, boolean administrator) {
        ServiceDispute dispute = disputeRepository.buscarDetalhe(disputeId);
        if (dispute == null || (!administrator && !participa(dispute, userId))) {
            throw notFound();
        }
        return toResponse(dispute, true);
    }

    @Transactional
    public DisputeResponse buscarPorOrcamento(Long quoteId, Integer userId, boolean administrator) {
        ServiceDispute dispute = disputeRepository.buscarPorOrcamento(quoteId);
        if (dispute == null) {
            ServiceQuote quote = quoteRepository.findById(quoteId);
            if (quote == null || (!administrator
                    && !Objects.equals(quote.getSolicitacao().getUsuario().getId(), userId)
                    && !Objects.equals(quote.getPrestador().getId(), userId))) {
                throw notFound();
            }
            return null;
        }
        if (!administrator && !participa(dispute, userId)) {
            throw notFound();
        }
        return toResponse(dispute, true);
    }

    @Transactional
    public DisputeResponse abrir(Integer clientId, Long quoteId, String reason) {
        String validatedReason = ServiceDisputePolicy.validarTexto(reason, "O motivo");
        Long requestId = quoteRepository.buscarSolicitacaoId(quoteId);
        if (requestId == null) {
            throw notFound();
        }
        ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
        ServiceQuote quote = quoteRepository.findById(quoteId);
        if (request == null || quote == null
                || !Objects.equals(request.getUsuario().getId(), clientId)) {
            throw notFound();
        }
        ServicePayment payment = paymentRepository.buscarPorOrcamentoComBloqueio(quoteId);
        ServiceExecution execution = executionRepository.buscarPorOrcamentoComBloqueio(quoteId);
        if (payment == null || execution == null) {
            throw conflict("O serviço pago não está disponível para contestação.");
        }
        ServiceDisputePolicy.validarAbertura(
                execution.getStatus(), execution.getIniciadoEm(), execution.getAprovadoEm());
        if (payment.getStatus() != PaymentStatus.PAGO
                && payment.getStatus() != PaymentStatus.REPASSE_PENDENTE) {
            throw conflict("O pagamento não permite abrir uma disputa.");
        }
        if (disputeRepository.buscarPorOrcamento(quoteId) != null) {
            throw conflict("Já existe uma disputa para este serviço.");
        }

        LocalDateTime now = LocalDateTime.now();
        ServiceDispute dispute = new ServiceDispute();
        dispute.setOrcamento(quote);
        dispute.setCliente(request.getUsuario());
        dispute.setMotivo(validatedReason);
        dispute.setEvidenciasAte(now.plusDays(ServiceDisputePolicy.EVIDENCE_WINDOW_DAYS));
        disputeRepository.persist(dispute);
        disputeRepository.flush();
        transicionarDisputa(dispute, null, ServiceDisputeStatus.ABERTA,
                LifecycleStatusTransitionPolicy.CLIENTE, request.getUsuario());
        transicionarExecucao(execution, ServiceExecutionStatus.CONTESTADA,
                LifecycleStatusTransitionPolicy.CLIENTE, request.getUsuario());

        notificarPartes(dispute, "DISPUTA_ABERTA",
                "Uma disputa foi aberta para o serviço. As partes têm 7 dias corridos para anexar evidências.");
        return toResponse(dispute, true);
    }

    @Transactional
    public EvidenceResponse anexarEvidencia(
            Long disputeId,
            Integer userId,
            boolean administrator,
            String filename,
            String contentType,
            byte[] content) {
        ServiceDispute dispute = disputeRepository.buscarPorIdComBloqueio(disputeId);
        if (dispute == null || (!administrator && !participa(dispute, userId))) {
            throw notFound();
        }
        if (administrator) {
            throw new WebApplicationException("Administradores não podem anexar evidências em nome das partes.",
                    Response.Status.FORBIDDEN);
        }
        LocalDateTime now = LocalDateTime.now();
        ServiceDisputePolicy.validarPrazoEvidencia(
                disputeRepository.contarEvidenciasPorParticipante(disputeId, userId),
                dispute.getEvidenciasAte(),
                now);
        if (dispute.getStatus() != ServiceDisputeStatus.ABERTA) {
            throw conflict("A disputa já foi encerrada.");
        }
        ServiceDisputePolicy.ValidatedEvidence validated =
                ServiceDisputePolicy.validarEvidencia(filename, contentType, content);
        ServiceDisputeEvidence evidence = new ServiceDisputeEvidence();
        evidence.setDisputa(dispute);
        User uploader = userRepository.buscarUsuarioPorId(userId);
        if (uploader == null) {
            throw notFound();
        }
        evidence.setEnviadoPor(uploader);
        evidence.setNomeArquivo(validated.filename());
        evidence.setTipoConteudo(validated.contentType());
        evidence.setTamanhoBytes(validated.content().length);
        evidence.setConteudo(validated.content());
        disputeRepository.getEntityManager().persist(evidence);
        disputeRepository.getEntityManager().flush();
        return toEvidenceResponse(evidence);
    }

    @Transactional
    public List<EvidenceResponse> listarEvidencias(Long disputeId, Integer userId, boolean administrator) {
        ServiceDispute dispute = disputeRepository.buscarDetalhe(disputeId);
        if (dispute == null || (!administrator && !participa(dispute, userId))) {
            throw notFound();
        }
        return metadados(disputeId);
    }

    @Transactional
    public EvidenceDownload baixarEvidencia(
            Long disputeId,
            Long evidenceId,
            Integer userId,
            boolean administrator) {
        ServiceDispute dispute = disputeRepository.buscarDetalhe(disputeId);
        if (dispute == null || (!administrator && !participa(dispute, userId))) {
            throw notFound();
        }
        ServiceDisputeEvidence evidence = disputeRepository.buscarEvidencia(disputeId, evidenceId);
        if (evidence == null) {
            throw notFound();
        }
        return new EvidenceDownload(evidence.getNomeArquivo(), evidence.getTipoConteudo(), evidence.getConteudo());
    }

    @Transactional
    public DisputeResponse decidir(
            Long disputeId,
            Integer adminId,
            ServiceDisputeDecision decision,
            String reason) {
        String validatedReason = ServiceDisputePolicy.validarTexto(reason, "A justificativa");
        ServiceDispute initial = disputeRepository.buscarDetalhe(disputeId);
        if (initial == null) {
            throw notFound();
        }
        Long quoteId = initial.getOrcamento().getId();
        Long requestId = quoteRepository.buscarSolicitacaoId(quoteId);
        if (requestId == null) {
            throw notFound();
        }
        ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
        ServicePayment payment = paymentRepository.buscarPorOrcamentoComBloqueio(quoteId);
        ServiceExecution execution = executionRepository.buscarPorOrcamentoComBloqueio(quoteId);
        ServiceDispute dispute = disputeRepository.buscarPorIdComBloqueio(disputeId);
        User administrator = userRepository.buscarUsuarioPorId(adminId);
        if (request == null || payment == null || execution == null || dispute == null
                || administrator == null
                || administrator.getPerfil() != com.acabaaqui.model.PerfilUser.administrador) {
            throw notFound();
        }
        if (dispute.getStatus() != ServiceDisputeStatus.ABERTA) {
            if (dispute.getDecisao() == decision) {
                return toResponse(dispute, true);
            }
            throw conflict("A disputa já foi encerrada com outra decisão.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (decision == ServiceDisputeDecision.REEMBOLSAR_CLIENTE) {
            PaymentStatus previousPaymentStatus = payment.getStatus();
            paymentService.reembolsar(payment.getId(), adminId);
            if (payment.getStatus() == PaymentStatus.REEMBOLSO_PENDENTE) {
                if (previousPaymentStatus != PaymentStatus.REEMBOLSO_PENDENTE) {
                    notificarPartes(dispute, "DISPUTA_REEMBOLSO_PENDENTE",
                            "O reembolso foi solicitado ao Mercado Pago e ainda está em processamento. A disputa continua aberta.");
                }
                return toResponse(dispute, true);
            }
            if (payment.getStatus() != PaymentStatus.REEMBOLSADO) {
                throw conflict("O Mercado Pago ainda não confirmou o reembolso. A disputa continua aberta.");
            }
            payoutService.cancelarRepassePendenteAposReembolso(quoteId);
            encerrar(dispute, ServiceDisputeStatus.ENCERRADA_REEMBOLSO,
                    decision, validatedReason, administrator, now);
        } else if (decision == ServiceDisputeDecision.PAGAR_PRESTADOR) {
            if (execution.getStatus() != ServiceExecutionStatus.CONTESTADA) {
                throw conflict("A execução não está aguardando decisão de disputa.");
            }
            if (payment.getStatus() != PaymentStatus.PAGO
                    && payment.getStatus() != PaymentStatus.REPASSE_PENDENTE) {
                throw conflict("O estado financeiro atual não permite liberar o repasse.");
            }
            transicionarExecucao(execution, ServiceExecutionStatus.APROVADA,
                    LifecycleStatusTransitionPolicy.ADMINISTRADOR, administrator);
            execution.setAprovadoEm(now);
            encerrar(dispute, ServiceDisputeStatus.ENCERRADA_REPASSE,
                    decision, validatedReason, administrator, now);
            if (payment.getStatus() == PaymentStatus.PAGO
                    && !payoutService.enfileirarAposAprovacao(quoteId)) {
                throw conflict("Não foi possível criar a pendência de repasse após a decisão.");
            }
            if (payment.getStatus() == PaymentStatus.REPASSE_PENDENTE
                    && !payoutService.existeRepassePendente(quoteId)) {
                throw conflict("O pagamento está pendente de repasse, mas a pendência administrativa não existe.");
            }
        } else {
            throw new IllegalArgumentException("Selecione uma decisão válida.");
        }

        notificarPartes(dispute, "DISPUTA_DECIDIDA",
                decision == ServiceDisputeDecision.REEMBOLSAR_CLIENTE
                        ? "A disputa foi encerrada com decisão de reembolso ao cliente."
                        : "A disputa foi encerrada com decisão de repasse ao prestador.");
        return toResponse(dispute, true);
    }

    private void encerrar(
            ServiceDispute dispute,
            ServiceDisputeStatus status,
            ServiceDisputeDecision decision,
            String reason,
            User administrator,
            LocalDateTime now) {
        transicionarDisputa(dispute, dispute.getStatus(), status,
                LifecycleStatusTransitionPolicy.ADMINISTRADOR, administrator);
        dispute.setStatus(status);
        dispute.setDecisao(decision);
        dispute.setJustificativaDecisao(reason);
        dispute.setAdministrador(administrator);
        dispute.setEncerradaEm(now);
    }

    private void transicionarDisputa(
            ServiceDispute dispute,
            ServiceDisputeStatus previous,
            ServiceDisputeStatus next,
            String actorType,
            User actor) {
        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.DISPUTA,
                dispute.getOrcamento().getId(),
                previous == null ? null : previous.name(),
                next.name(),
                actorType,
                actor);
    }

    private void transicionarExecucao(
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

    private void notificarPartes(ServiceDispute dispute, String type, String message) {
        ServiceQuote quote = dispute.getOrcamento();
        ServiceRequest request = quote.getSolicitacao();
        notificationService.criar(request.getUsuario(), request, quote, type, message);
        notificationService.criar(quote.getPrestador(), request, quote, type, message);
    }

    private boolean participa(ServiceDispute dispute, Integer userId) {
        return Objects.equals(dispute.getOrcamento().getSolicitacao().getUsuario().getId(), userId)
                || Objects.equals(dispute.getOrcamento().getPrestador().getId(), userId);
    }

    private DisputeResponse toResponse(ServiceDispute dispute, boolean includeEvidence) {
        ServiceQuote quote = dispute.getOrcamento();
        ServiceRequest request = quote.getSolicitacao();
        return new DisputeResponse(
                dispute.getId(),
                quote.getId(),
                request.getServico().getNome(),
                request.getUsuario().getId(),
                request.getUsuario().getNome(),
                quote.getPrestador().getId(),
                quote.getPrestador().getNome(),
                dispute.getMotivo(),
                dispute.getStatus().name(),
                dispute.getAbertaEm(),
                dispute.getEvidenciasAte(),
                dispute.getDecisao() == null ? null : dispute.getDecisao().name(),
                dispute.getJustificativaDecisao(),
                dispute.getAdministrador() == null ? null : dispute.getAdministrador().getNome(),
                dispute.getEncerradaEm(),
                paymentStatus(quote.getId()),
                includeEvidence ? metadados(dispute.getId()) : List.of());
    }

    private String paymentStatus(Long quoteId) {
        ServicePayment payment = paymentRepository.buscarPorOrcamento(quoteId);
        return payment == null ? null : payment.getStatus().name();
    }

    private List<EvidenceResponse> metadados(Long disputeId) {
        return disputeRepository.listarMetadadosEvidencias(disputeId).stream()
                .map(row -> new EvidenceResponse(
                        (Long) row[0],
                        (Integer) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        ((Number) row[5]).longValue(),
                        (LocalDateTime) row[6]))
                .toList();
    }

    private EvidenceResponse toEvidenceResponse(ServiceDisputeEvidence evidence) {
        return new EvidenceResponse(
                evidence.getId(),
                evidence.getEnviadoPor().getId(),
                evidence.getEnviadoPor().getNome(),
                evidence.getNomeArquivo(),
                evidence.getTipoConteudo(),
                evidence.getTamanhoBytes(),
                evidence.getCriadaEm());
    }

    private WebApplicationException notFound() {
        return new WebApplicationException("Disputa não encontrada.", Response.Status.NOT_FOUND);
    }

    private WebApplicationException conflict(String message) {
        return new WebApplicationException(message, Response.Status.CONFLICT);
    }

    public record EvidenceResponse(
            Long id,
            Integer uploaderId,
            String uploaderName,
            String filename,
            String contentType,
            long sizeBytes,
            LocalDateTime createdAt) { }

    public record EvidenceDownload(String filename, String contentType, byte[] content) { }

    public record DisputeResponse(
            Long id,
            Long quoteId,
            String serviceName,
            Integer clientId,
            String clientName,
            Integer providerId,
            String providerName,
            String reason,
            String status,
            LocalDateTime openedAt,
            LocalDateTime evidenceDeadline,
            String decision,
            String decisionReason,
            String administratorName,
            LocalDateTime closedAt,
            String paymentStatus,
            List<EvidenceResponse> evidence) { }
}
