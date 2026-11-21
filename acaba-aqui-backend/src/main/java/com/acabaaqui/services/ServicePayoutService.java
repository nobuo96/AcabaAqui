package com.acabaaqui.services;

import com.acabaaqui.model.PaymentStatus;
import com.acabaaqui.model.PerfilUser;
import com.acabaaqui.model.PixKeyType;
import com.acabaaqui.model.ProviderPixKey;
import com.acabaaqui.model.ServiceExecution;
import com.acabaaqui.model.ServiceExecutionStatus;
import com.acabaaqui.model.ServicePayment;
import com.acabaaqui.model.ServicePayout;
import com.acabaaqui.model.ServicePayoutReason;
import com.acabaaqui.model.ServicePayoutStatus;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.LifecycleStatusHistoryRepository;
import com.acabaaqui.persistence.ProviderPixKeyRepository;
import com.acabaaqui.persistence.ServiceExecutionRepository;
import com.acabaaqui.persistence.ServicePaymentRepository;
import com.acabaaqui.persistence.ServicePayoutRepository;
import com.acabaaqui.persistence.ServiceQuoteRepository;
import com.acabaaqui.persistence.ServiceRequestRepository;
import com.acabaaqui.persistence.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ServicePayoutService {

    @Inject
    ProviderPixKeyRepository pixKeyRepository;

    @Inject
    ServicePayoutRepository payoutRepository;

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
    LifecycleStatusHistoryRepository historyRepository;

    @Inject
    LifecycleStatusService lifecycleStatusService;

    @Inject
    PixKeyCryptoService pixKeyCryptoService;

    @Inject
    InAppNotificationService notificationService;

    @Transactional
    public PixKeyResponse buscarChavePix(Integer providerId) {
        ProviderPixKey pixKey = pixKeyRepository.buscarPorPrestador(providerId);
        return toPixKeyResponse(pixKey);
    }

    @Transactional
    public PixKeyResponse atualizarChavePix(Integer providerId, PixKeyType type, String value) {
        String normalizedValue = PixKeyValidation.normalize(type, value);
        User provider = userRepository.buscarUsuarioPorIdComBloqueio(providerId);
        if (provider == null || provider.getPerfil() != PerfilUser.prestador) {
            throw new WebApplicationException("Prestador não encontrado.", Response.Status.NOT_FOUND);
        }
        ProviderPixKey pixKey = pixKeyRepository.buscarPorPrestadorComBloqueio(providerId);
        if (pixKey == null) {
            pixKey = new ProviderPixKey();
            pixKey.setPrestador(provider);
        }
        pixKey.setTipo(type);
        pixKey.setChaveCriptografada(pixKeyCryptoService.encrypt(normalizedValue));
        if (pixKey.getId() == null) {
            pixKeyRepository.persist(pixKey);
            pixKeyRepository.flush();
        }
        return toPixKeyResponse(pixKey);
    }

    @Transactional
    public List<PayoutResponse> listarRepasses() {
        return payoutRepository.listarTodos().stream()
                .map(payout -> toResponse(payout, false))
                .toList();
    }

    @Transactional
    public PayoutResponse buscarRepasse(Long payoutId) {
        ServicePayout payout = payoutRepository.buscarDetalhe(payoutId);
        if (payout == null) {
            throw notFound();
        }
        return toResponse(payout, payout.getStatus() == ServicePayoutStatus.PENDENTE);
    }

    @Transactional
    public PayoutResponse registrarPagamento(
            Long payoutId,
            Integer adminId,
            PayoutPaymentRequest request) {
        ServicePayout initialPayout = payoutRepository.buscarDetalhe(payoutId);
        if (initialPayout == null) {
            throw notFound();
        }
        Long quoteId = initialPayout.getExecucao().getOrcamento().getId();
        Long requestId = quoteRepository.buscarSolicitacaoId(quoteId);
        if (requestId == null) {
            throw notFound();
        }

        ServiceRequest serviceRequest = requestRepository.buscarComBloqueio(requestId);
        ServicePayment payment = paymentRepository.buscarPorOrcamentoComBloqueio(quoteId);
        ServiceExecution execution = executionRepository.buscarPorOrcamentoComBloqueio(quoteId);
        ServicePayout payout = payoutRepository.buscarPorIdComBloqueio(payoutId);
        if (serviceRequest == null || payment == null || execution == null || payout == null
                || !payout.getExecucao().getId().equals(quoteId)) {
            throw notFound();
        }
        User admin = userRepository.buscarUsuarioPorId(adminId);
        if (admin == null || admin.getPerfil() != PerfilUser.administrador) {
            throw new WebApplicationException("Administrador não encontrado.", Response.Status.NOT_FOUND);
        }
        if (payout.getStatus() == ServicePayoutStatus.PAGO) {
            return toResponse(payout, false);
        }

        ProviderPixKey pixKey = pixKeyRepository.buscarPorPrestadorComBloqueio(
                execution.getOrcamento().getPrestador().getId());
        boolean disputeOpen = existeDisputaAberta(quoteId);
        ServicePayoutReason eligibleReason =
                ServicePayoutPolicy.motivoElegibilidade(execution, LocalDateTime.now());
        if (eligibleReason == null) {
            throw conflict("O serviço ainda não atingiu a condição de elegibilidade para repasse.");
        }
        if (disputeOpen) {
            throw conflict("O repasse está bloqueado enquanto houver disputa aberta.");
        }
        if (pixKey == null) {
            throw conflict("O prestador ainda não cadastrou uma chave Pix.");
        }
        if (payment.getStatus() != PaymentStatus.REPASSE_PENDENTE) {
            throw conflict("O pagamento não está na fila de repasse.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Informe o valor e a referência da transferência.");
        }

        String transferReference = request.transferReference() == null
                ? null
                : request.transferReference().trim();
        String pixKeyValue = pixKeyCryptoService.decrypt(pixKey.getChaveCriptografada());
        if (!Objects.equals(pixKeyValue, request.expectedPixKey())) {
            throw conflict("A chave Pix mudou desde que os detalhes foram carregados. Atualize e confira novamente.");
        }
        BigDecimal amount = ServicePayoutPolicy.validarPagamento(
                payout.getStatus(),
                payment.getStatus(),
                false,
                request.amount(),
                payment.getValorPago(),
                transferReference);
        if (payoutRepository.existeReferenciaTransferencia(transferReference)) {
            throw conflict("Este identificador de transferência já foi registrado.");
        }
        String receiptReference = normalizarReferenciaComprovante(request.receiptReference());

        payout.setStatus(ServicePayoutStatus.PAGO);
        payout.setValorPago(amount);
        payout.setDestinatarioNome(execution.getOrcamento().getPrestador().getNome());
        payout.setDestinatarioTipoChave(pixKey.getTipo());
        payout.setDestinatarioChaveCriptografada(pixKeyCryptoService.encrypt(pixKeyValue));
        payout.setReferenciaTransferencia(transferReference);
        payout.setReferenciaComprovante(receiptReference);
        payout.setPagoEm(LocalDateTime.now());
        payout.setAdministrador(admin);

        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.PAGAMENTO,
                payment.getId(),
                PaymentStatus.REPASSE_PENDENTE.name(),
                PaymentStatus.REPASSADO.name(),
                LifecycleStatusTransitionPolicy.ADMINISTRADOR,
                admin);
        payment.setStatus(PaymentStatus.REPASSADO);
        notificationService.criar(
                execution.getOrcamento().getPrestador(),
                serviceRequest,
                execution.getOrcamento(),
                "REPASSE_REALIZADO",
                "O administrador registrou o repasse Pix do serviço.");
        return toResponse(payout, false);
    }

    @Transactional
    public boolean enfileirarAposAprovacao(Long quoteId) {
        return enfileirarSeElegivel(quoteId, LocalDateTime.now());
    }

    @Transactional
    public boolean enfileirarAposPrazo(Long quoteId, LocalDateTime now) {
        return enfileirarSeElegivel(quoteId, now);
    }

    @Transactional
    public boolean existeRepassePendente(Long quoteId) {
        ServicePayout payout = payoutRepository.buscarPorOrcamento(quoteId);
        return payout != null && payout.getStatus() == ServicePayoutStatus.PENDENTE;
    }

    @Transactional
    public void cancelarRepassePendenteAposReembolso(Long quoteId) {
        ServicePayout payout = payoutRepository.buscarPorOrcamentoComBloqueio(quoteId);
        if (payout != null && payout.getStatus() == ServicePayoutStatus.PENDENTE) {
            payout.setStatus(ServicePayoutStatus.CANCELADO);
        }
    }

    private boolean enfileirarSeElegivel(Long quoteId, LocalDateTime now) {
        Long requestId = quoteRepository.buscarSolicitacaoId(quoteId);
        if (requestId == null) {
            return false;
        }
        ServiceRequest serviceRequest = requestRepository.buscarComBloqueio(requestId);
        ServicePayment payment = paymentRepository.buscarPorOrcamentoComBloqueio(quoteId);
        ServiceExecution execution = executionRepository.buscarPorOrcamentoComBloqueio(quoteId);
        if (serviceRequest == null || payment == null || execution == null) {
            return false;
        }
        ServicePayout existing = payoutRepository.buscarPorOrcamentoComBloqueio(quoteId);
        if (existing != null) {
            return true;
        }

        ServicePayoutReason reason = ServicePayoutPolicy.motivoElegibilidade(execution, now);
        boolean disputeOpen = existeDisputaAberta(quoteId);
        if (reason == null || payment.getStatus() != PaymentStatus.PAGO || disputeOpen) {
            return false;
        }
        ServicePayoutPolicy.validarCriacao(payment.getStatus(), false, reason);
        ServicePayout payout = new ServicePayout();
        payout.setExecucao(execution);
        payout.setMotivoElegibilidade(reason);
        payout.setElegivelEm(reason == ServicePayoutReason.APROVACAO_CLIENTE
                ? execution.getAprovadoEm()
                : execution.getAprovarAte());
        payoutRepository.persist(payout);
        payoutRepository.flush();

        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.PAGAMENTO,
                payment.getId(),
                PaymentStatus.PAGO.name(),
                PaymentStatus.REPASSE_PENDENTE.name(),
                LifecycleStatusTransitionPolicy.SISTEMA,
                null);
        payment.setStatus(PaymentStatus.REPASSE_PENDENTE);
        notificationService.criar(
                execution.getOrcamento().getPrestador(),
                serviceRequest,
                execution.getOrcamento(),
                "REPASSE_PENDENTE",
                "O serviço está elegível para repasse Pix manual pela equipe da plataforma.");
        return true;
    }

    private boolean existeDisputaAberta(Long quoteId) {
        return "ABERTA".equals(historyRepository.buscarUltimoStatus(
                LifecycleStatusTransitionPolicy.DISPUTA, quoteId));
    }

    private PixKeyResponse toPixKeyResponse(ProviderPixKey pixKey) {
        if (pixKey == null) {
            return new PixKeyResponse(false, null, null, null);
        }
        String value = pixKeyCryptoService.decrypt(pixKey.getChaveCriptografada());
        return new PixKeyResponse(
                true,
                pixKey.getTipo().name(),
                PixKeyValidation.mascarar(pixKey.getTipo(), value),
                pixKey.getUltimaAtualizacao());
    }

    private PayoutResponse toResponse(ServicePayout payout, boolean revealCurrentKey) {
        ServiceExecution execution = payout.getExecucao();
        Long quoteId = execution.getOrcamento().getId();
        ServicePayment payment = paymentRepository.buscarPorOrcamento(quoteId);
        boolean disputeOpen = existeDisputaAberta(quoteId);
        String pixType = null;
        String pixMasked = null;
        String pixValue = null;
        ProviderPixKey currentKey = null;

        if (payout.getStatus() == ServicePayoutStatus.PENDENTE) {
            currentKey = pixKeyRepository.buscarPorPrestador(
                    execution.getOrcamento().getPrestador().getId());
        }
        if (payout.getStatus() == ServicePayoutStatus.PAGO
                && payout.getDestinatarioChaveCriptografada() != null) {
            pixType = payout.getDestinatarioTipoChave().name();
            String value = pixKeyCryptoService.decrypt(payout.getDestinatarioChaveCriptografada());
            pixMasked = PixKeyValidation.mascarar(payout.getDestinatarioTipoChave(), value);
        } else if (currentKey != null) {
            pixType = currentKey.getTipo().name();
            String value = pixKeyCryptoService.decrypt(currentKey.getChaveCriptografada());
            pixMasked = PixKeyValidation.mascarar(currentKey.getTipo(), value);
            if (revealCurrentKey && !disputeOpen) {
                pixValue = value;
            }
        }

        boolean canPay = payout.getStatus() == ServicePayoutStatus.PENDENTE
                && !disputeOpen
                && currentKey != null
                && payment != null
                && payment.getStatus() == PaymentStatus.REPASSE_PENDENTE;
        return new PayoutResponse(
                payout.getId(),
                quoteId,
                execution.getOrcamento().getSolicitacao().getServico().getNome(),
                execution.getOrcamento().getPrestador().getId(),
                execution.getOrcamento().getPrestador().getNome(),
                payment == null ? null : payment.getValorPago(),
                payment == null ? null : payment.getStatus().name(),
                payout.getStatus().name(),
                payout.getMotivoElegibilidade().name(),
                payout.getElegivelEm(),
                execution.getAprovarAte(),
                pixType,
                pixMasked,
                pixValue,
                disputeOpen,
                canPay,
                payout.getValorPago(),
                payout.getReferenciaTransferencia(),
                payout.getReferenciaComprovante(),
                payout.getPagoEm(),
                payout.getAdministrador() == null ? null : payout.getAdministrador().getNome());
    }

    private String normalizarReferenciaComprovante(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 1000) {
            throw new IllegalArgumentException("A referência do comprovante deve ter até 1000 caracteres.");
        }
        return trimmed;
    }

    private WebApplicationException notFound() {
        return new WebApplicationException("Repasse não encontrado.", Response.Status.NOT_FOUND);
    }

    private WebApplicationException conflict(String message) {
        return new WebApplicationException(message, Response.Status.CONFLICT);
    }

    public record PixKeyResponse(
            boolean hasKey,
            String type,
            String maskedValue,
            LocalDateTime updatedAt) { }

    public record PayoutPaymentRequest(
            BigDecimal amount,
            String transferReference,
            String receiptReference,
            String expectedPixKey) { }

    public record PayoutResponse(
            Long id,
            Long quoteId,
            String serviceName,
            Integer providerId,
            String providerName,
            BigDecimal paymentAmount,
            String paymentStatus,
            String status,
            String reason,
            LocalDateTime eligibleAt,
            LocalDateTime approvalDeadline,
            String pixKeyType,
            String pixKeyMasked,
            String pixKeyValue,
            boolean disputeOpen,
            boolean canPay,
            BigDecimal transferredAmount,
            String transferReference,
            String receiptReference,
            LocalDateTime paidAt,
            String administratorName) { }
}
