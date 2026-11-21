package com.acabaaqui.services;

import com.acabaaqui.model.PaymentStatus;
import com.acabaaqui.model.ServiceExecution;
import com.acabaaqui.model.ServiceExecutionStatus;
import com.acabaaqui.model.ServicePayment;
import com.acabaaqui.model.ServiceQuote;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceWarranty;
import com.acabaaqui.model.ServiceWarrantyStatus;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.ServiceExecutionRepository;
import com.acabaaqui.persistence.ServicePaymentRepository;
import com.acabaaqui.persistence.ServiceQuoteRepository;
import com.acabaaqui.persistence.ServiceRequestRepository;
import com.acabaaqui.persistence.ServiceWarrantyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ServiceWarrantyService {

    private static final Logger LOG = Logger.getLogger(ServiceWarrantyService.class);

    @Inject
    ServiceWarrantyRepository warrantyRepository;

    @Inject
    ServiceQuoteRepository quoteRepository;

    @Inject
    ServiceRequestRepository requestRepository;

    @Inject
    ServiceExecutionRepository executionRepository;

    @Inject
    ServicePaymentRepository paymentRepository;

    @Inject
    LifecycleStatusService lifecycleStatusService;

    @Inject
    InAppNotificationService notificationService;

    @Inject
    ServicePaymentService paymentService;

    @Inject
    ServicePayoutService payoutService;

    @Transactional
    public void ativar(ServiceExecution execution, LocalDateTime approvedAt) {
        ServiceQuote quote = execution.getOrcamento();
        if (quote.getGarantiaDias() <= 0
                || warrantyRepository.buscarPorOrcamentoComBloqueio(quote.getId()) != null) {
            return;
        }
        ServiceWarranty warranty = new ServiceWarranty();
        warranty.setOrcamento(quote);
        warranty.setIniciadaEm(approvedAt);
        warranty.setTerminaEm(approvedAt.plusDays(quote.getGarantiaDias()));
        warrantyRepository.persist(warranty);
        warrantyRepository.flush();
        registrarTransicao(warranty, null, ServiceWarrantyStatus.ATIVA,
                LifecycleStatusTransitionPolicy.SISTEMA, null);
    }

    @Transactional
    public WarrantyResponse buscarPorOrcamento(Long quoteId, Integer userId) {
        ServiceQuote quote = quoteRepository.findById(quoteId);
        if (quote == null || (!Objects.equals(quote.getSolicitacao().getUsuario().getId(), userId)
                && !Objects.equals(quote.getPrestador().getId(), userId))) {
            throw notFound();
        }
        ServiceWarranty warranty = warrantyRepository.buscarPorOrcamento(quoteId);
        return warranty == null ? null : toResponse(warranty);
    }

    @Transactional
    public WarrantyResponse abrir(Integer clientId, Long quoteId, String reason, Clock clock) {
        String validatedReason = ServiceWarrantyPolicy.validarMotivo(reason);
        Long requestId = quoteRepository.buscarSolicitacaoId(quoteId);
        if (requestId == null) {
            throw notFound();
        }
        ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
        ServiceQuote quote = quoteRepository.findById(quoteId);
        ServiceExecution execution = executionRepository.buscarPorOrcamentoComBloqueio(quoteId);
        ServicePayment payment = paymentRepository.buscarPorOrcamentoComBloqueio(quoteId);
        ServiceWarranty warranty = warrantyRepository.buscarPorOrcamentoComBloqueio(quoteId);
        if (request == null || quote == null
                || !Objects.equals(request.getUsuario().getId(), clientId)) {
            throw notFound();
        }
        if (execution == null || execution.getStatus() != ServiceExecutionStatus.APROVADA
                || payment == null || !pagamentoElegivel(payment.getStatus())
                || warranty == null) {
            throw conflict("A garantia deste serviço não está disponível.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (warranty != null
                && warranty.getStatus() == ServiceWarrantyStatus.RESOLVIDA
                && !now.isAfter(warranty.getTerminaEm())) {
            LocalDateTime startsAt = warranty.getIniciadaEm();
            LocalDateTime endsAt = warranty.getTerminaEm();
            warranty = new ServiceWarranty();
            warranty.setOrcamento(quote);
            warranty.setIniciadaEm(startsAt);
            warranty.setTerminaEm(endsAt);
            warrantyRepository.persist(warranty);
            warrantyRepository.flush();
            registrarTransicao(warranty, null, ServiceWarrantyStatus.ATIVA,
                    LifecycleStatusTransitionPolicy.SISTEMA, null);
        }
        ServiceWarrantyPolicy.validarAbertura(
                warranty == null ? null : warranty.getStatus(),
                warranty == null ? null : warranty.getIniciadaEm(),
                warranty == null ? null : warranty.getTerminaEm(),
                now);
        warranty.setMotivoChamado(validatedReason);
        warranty.setAcionadaEm(now);
        warranty.setCorrecaoAte(ServiceWarrantyPolicy.prazoCorrecao(now));
        transicionar(warranty, ServiceWarrantyStatus.ACIONADA,
                LifecycleStatusTransitionPolicy.CLIENTE, request.getUsuario());
        notificarPartes(warranty, "GARANTIA_ACIONADA",
                "O cliente abriu um chamado de garantia. O prazo para correção é de 7 dias corridos.");
        return toResponse(warranty);
    }

    @Transactional
    public WarrantyResponse corrigir(Integer providerId, Long warrantyId, Clock clock) {
        ServiceWarranty warranty = bloquearWarranty(warrantyId);
        ServiceQuote quote = warranty.getOrcamento();
        if (!Objects.equals(quote.getPrestador().getId(), providerId)) {
            throw notFound();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        ServiceWarrantyPolicy.validarCorrecao(warranty.getStatus(), warranty.getCorrecaoAte(), now);
        warranty.setCorrigidaEm(now);
        transicionar(warranty, ServiceWarrantyStatus.CORRIGIDA,
                LifecycleStatusTransitionPolicy.PRESTADOR, quote.getPrestador());
        notificarCliente(warranty, "GARANTIA_CORRIGIDA",
                "O prestador informou que corrigiu o problema. Confirme se o serviço foi resolvido.");
        return toResponse(warranty);
    }

    @Transactional
    public WarrantyResponse resolver(Integer clientId, Long warrantyId, Clock clock) {
        ServiceWarranty warranty = bloquearWarranty(warrantyId);
        ServiceQuote quote = warranty.getOrcamento();
        ServiceRequest request = quote.getSolicitacao();
        if (!Objects.equals(request.getUsuario().getId(), clientId)) {
            throw notFound();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        ServiceWarrantyPolicy.validarResolucao(warranty.getStatus(), warranty.getCorrecaoAte(), now);
        warranty.setResolvidaEm(now);
        transicionar(warranty, ServiceWarrantyStatus.RESOLVIDA,
                LifecycleStatusTransitionPolicy.CLIENTE, request.getUsuario());
        notificarPrestador(warranty, "GARANTIA_RESOLVIDA",
                "O cliente confirmou que o chamado de garantia foi resolvido.");
        return toResponse(warranty);
    }

    @Transactional
    public int marcarChamadosVencidos(Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        int marked = 0;
        for (Long warrantyId : warrantyRepository.listarChamadosVencidos(now)) {
            ServiceWarranty initial = warrantyRepository.buscarDetalhe(warrantyId);
            if (initial == null) {
                continue;
            }
            ServiceRequest request = requestRepository.buscarComBloqueio(
                    initial.getOrcamento().getSolicitacao().getId());
            ServiceWarranty warranty = warrantyRepository.buscarPorIdComBloqueio(warrantyId);
            if (request == null || warranty == null
                    || warranty.getStatus() != ServiceWarrantyStatus.ACIONADA
                    || warranty.getCorrecaoAte().isAfter(now)) {
                continue;
            }
            transicionar(warranty, ServiceWarrantyStatus.REEMBOLSO_PENDENTE,
                    LifecycleStatusTransitionPolicy.SISTEMA, null);
            notificarPartes(warranty, "GARANTIA_REEMBOLSO_PENDENTE",
                    "O prazo de correção terminou. A plataforma está processando o reembolso integral.");
            marked++;
        }
        return marked;
    }

    @Transactional
    public void tentarReembolso(Long warrantyId) {
        ServiceWarranty initial = warrantyRepository.buscarDetalhe(warrantyId);
        if (initial == null) {
            return;
        }
        ServiceQuote quote = initial.getOrcamento();
        ServiceRequest request = requestRepository.buscarComBloqueio(quote.getSolicitacao().getId());
        ServicePayment payment = paymentRepository.buscarPorOrcamentoComBloqueio(quote.getId());
        ServiceWarranty warranty = warrantyRepository.buscarPorIdComBloqueio(warrantyId);
        if (request == null || payment == null || warranty == null
                || warranty.getStatus() != ServiceWarrantyStatus.REEMBOLSO_PENDENTE) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.REEMBOLSADO
                && payment.getStatus() != PaymentStatus.REEMBOLSADO_PARCIAL) {
            paymentService.reembolsarPorGarantia(payment.getId());
        }
        if (payment.getStatus() == PaymentStatus.REEMBOLSADO) {
            payoutService.cancelarRepassePendenteAposReembolso(quote.getId());
            warranty.setReembolsadaEm(LocalDateTime.now());
            transicionar(warranty, ServiceWarrantyStatus.REEMBOLSADA,
                    LifecycleStatusTransitionPolicy.MERCADO_PAGO, null);
            notificarPartes(warranty, "GARANTIA_REEMBOLSADA",
                    "O Mercado Pago confirmou o reembolso integral referente à garantia.");
        } else if (payment.getStatus() == PaymentStatus.REEMBOLSADO_PARCIAL) {
            transicionar(warranty, ServiceWarrantyStatus.FALHA_REEMBOLSO,
                    LifecycleStatusTransitionPolicy.SISTEMA, null);
            notificarPartes(warranty, "GARANTIA_FALHA_REEMBOLSO",
                    "O Mercado Pago confirmou apenas um reembolso parcial. A equipe da plataforma precisa analisar o caso.");
            LOG.errorf("Warranty %d refund was only partial for payment %d.",
                    warrantyId, payment.getId());
        }
    }

    @Transactional
    public List<Long> listarReembolsosPendentes() {
        return warrantyRepository.listarReembolsosPendentes();
    }

    private ServiceWarranty bloquearWarranty(Long warrantyId) {
        ServiceWarranty initial = warrantyRepository.buscarDetalhe(warrantyId);
        if (initial == null) {
            throw notFound();
        }
        ServiceQuote quote = initial.getOrcamento();
        requestRepository.buscarComBloqueio(quote.getSolicitacao().getId());
        ServiceWarranty warranty = warrantyRepository.buscarPorIdComBloqueio(warrantyId);
        if (warranty == null) {
            throw notFound();
        }
        return warranty;
    }

    private void transicionar(
            ServiceWarranty warranty,
            ServiceWarrantyStatus next,
            String actorType,
            User actor) {
        registrarTransicao(warranty, warranty.getStatus(), next, actorType, actor);
        warranty.setStatus(next);
    }

    private void registrarTransicao(
            ServiceWarranty warranty,
            ServiceWarrantyStatus previous,
            ServiceWarrantyStatus next,
            String actorType,
            User actor) {
        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.GARANTIA,
                warranty.getId(),
                previous == null ? null : previous.name(),
                next.name(),
                actorType,
                actor);
    }

    private void notificarPartes(ServiceWarranty warranty, String type, String message) {
        notificarCliente(warranty, type, message);
        notificarPrestador(warranty, type, message);
    }

    private void notificarCliente(ServiceWarranty warranty, String type, String message) {
        ServiceQuote quote = warranty.getOrcamento();
        notificationService.criar(
                quote.getSolicitacao().getUsuario(), quote.getSolicitacao(), quote, type, message);
    }

    private void notificarPrestador(ServiceWarranty warranty, String type, String message) {
        ServiceQuote quote = warranty.getOrcamento();
        notificationService.criar(quote.getPrestador(), quote.getSolicitacao(), quote, type, message);
    }

    private WarrantyResponse toResponse(ServiceWarranty warranty) {
        ServiceQuote quote = warranty.getOrcamento();
        ServicePayment payment = paymentRepository.buscarPorOrcamento(quote.getId());
        return new WarrantyResponse(
                warranty.getId(),
                quote.getId(),
                quote.getGarantiaDias(),
                warranty.getStatus().name(),
                warranty.getIniciadaEm(),
                warranty.getTerminaEm(),
                warranty.getMotivoChamado(),
                warranty.getAcionadaEm(),
                warranty.getCorrecaoAte(),
                warranty.getCorrigidaEm(),
                warranty.getResolvidaEm(),
                warranty.getReembolsadaEm(),
                payment == null ? null : payment.getStatus().name());
    }

    private boolean pagamentoElegivel(PaymentStatus status) {
        return status == PaymentStatus.PAGO
                || status == PaymentStatus.REPASSE_PENDENTE
                || status == PaymentStatus.REPASSADO;
    }

    private WebApplicationException notFound() {
        return new WebApplicationException("Garantia não encontrada.", Response.Status.NOT_FOUND);
    }

    private WebApplicationException conflict(String message) {
        return new WebApplicationException(message, Response.Status.CONFLICT);
    }

    public record WarrantyResponse(
            Long id,
            Long quoteId,
            int warrantyDays,
            String status,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            String reason,
            LocalDateTime openedAt,
            LocalDateTime correctionDeadline,
            LocalDateTime correctedAt,
            LocalDateTime resolvedAt,
            LocalDateTime refundedAt,
            String paymentStatus) { }
}
