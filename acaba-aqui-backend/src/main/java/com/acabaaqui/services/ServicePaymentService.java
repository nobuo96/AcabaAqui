package com.acabaaqui.services;

import com.acabaaqui.model.PaymentStatus;
import com.acabaaqui.model.ServiceCancellation;
import com.acabaaqui.model.ServiceCancellationStatus;
import com.acabaaqui.model.ServicePayment;
import com.acabaaqui.model.ServicePaymentOrderAttempt;
import com.acabaaqui.model.ServiceQuote;
import com.acabaaqui.model.ServiceQuoteStatus;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.ServicePaymentRepository;
import com.acabaaqui.persistence.ServicePaymentOrderAttemptRepository;
import com.acabaaqui.persistence.ServiceCancellationRepository;
import com.acabaaqui.persistence.LifecycleStatusHistoryRepository;
import com.acabaaqui.persistence.ServiceQuoteRepository;
import com.acabaaqui.persistence.ServiceRequestRepository;
import com.acabaaqui.persistence.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class ServicePaymentService {

    private static final Logger LOG = Logger.getLogger(ServicePaymentService.class);
    private static final String DEFAULT_RETURN_URL = "acabaaquifrontend://payment-return";

    @ConfigProperty(name = "mercadopago.access-token", defaultValue = "")
    String accessToken;

    @ConfigProperty(name = "mercadopago.payment.return-url", defaultValue = DEFAULT_RETURN_URL)
    String returnUrlBase;

    @Inject
    ServicePaymentRepository paymentRepository;

    @Inject
    ServicePaymentOrderAttemptRepository attemptRepository;

    @Inject
    ServiceCancellationRepository cancellationRepository;

    @Inject
    LifecycleStatusHistoryRepository lifecycleHistoryRepository;

    @Inject
    ServiceQuoteRepository quoteRepository;

    @Inject
    ServiceRequestRepository requestRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    MercadoPagoOrderGateway orderGateway;

    @Inject
    MercadoPagoWebhookSignatureVerifier signatureVerifier;

    @Inject
    LifecycleStatusService lifecycleStatusService;

    @Inject
    InAppNotificationService notificationService;

    @Inject
    ServiceExecutionService executionService;

    @Transactional
    public PaymentResponse iniciarCheckout(Integer clientId, Long quoteId) {
        validarAccessToken();
        Long requestId = quoteRepository.buscarSolicitacaoId(quoteId);
        if (requestId == null) {
            throw notFound();
        }
        ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
        ServiceQuote quote = quoteRepository.findById(quoteId);
        if (request == null || quote == null || !request.getUsuario().getId().equals(clientId)) {
            throw notFound();
        }
        if (quote.getStatus() != ServiceQuoteStatus.APROVADO) {
            throw conflict("Somente um orçamento aprovado pode ser pago.");
        }

        ServicePayment payment = paymentRepository.buscarPorOrcamentoComBloqueio(quoteId);
        if (payment != null) {
            if (payment.getMercadoPagoOrderId() != null) {
                if (payment.getStatus() == PaymentStatus.AGUARDANDO_PAGAMENTO) {
                    sincronizar(payment);
                    return toResponse(payment);
                }
                if (!permiteNovaTentativa(payment.getStatus())) {
                    throw conflict("Este orçamento já possui um pagamento finalizado.");
                }
                PaymentStatus previousStatus = payment.getStatus();
                payment.setTentativa(payment.getTentativa() + 1);
                payment.setStatus(PaymentStatus.AGUARDANDO_PAGAMENTO);
                payment.setMercadoPagoOrderId(null);
                payment.setCheckoutUrl(null);
                payment.setOrderStatus(null);
                payment.setStatusDetail(null);
                payment.setValorPago(BigDecimal.ZERO.setScale(2));
                lifecycleStatusService.registrarTransicao(
                        LifecycleStatusTransitionPolicy.PAGAMENTO,
                        payment.getId(),
                        previousStatus.name(),
                        PaymentStatus.AGUARDANDO_PAGAMENTO.name(),
                        LifecycleStatusTransitionPolicy.CLIENTE,
                        request.getUsuario());
            }
        } else {
            payment = new ServicePayment();
            payment.setOrcamento(quote);
            payment.setValorTotal(quote.getValorTotal().setScale(2));
            payment.setValorPago(BigDecimal.ZERO.setScale(2));
            payment.setStatus(PaymentStatus.AGUARDANDO_PAGAMENTO);
            UUID idempotencyKey = chaveDeterministica("payment-1", quoteId);
            payment.setIdempotencyKey(idempotencyKey.toString());
            payment.setExternalReference(idempotencyKey.toString());
            paymentRepository.persist(payment);
            paymentRepository.flush();
            lifecycleStatusService.registrarTransicao(
                    LifecycleStatusTransitionPolicy.PAGAMENTO,
                    payment.getId(),
                    null,
                    PaymentStatus.AGUARDANDO_PAGAMENTO.name(),
                    LifecycleStatusTransitionPolicy.SISTEMA,
                    null);
        }

        UUID idempotencyKey = chaveDeterministica("payment-" + payment.getTentativa(), quoteId);
        payment.setIdempotencyKey(idempotencyKey.toString());
        payment.setExternalReference(idempotencyKey.toString());
        MercadoPagoOrderGateway.CheckoutOrder order = orderGateway.criarPedido(
                accessToken,
                idempotencyKey,
                payment.getExternalReference(),
                "Pagamento do serviço " + quote.getSolicitacao().getServico().getNome(),
                payment.getValorTotal(),
                returnUrls(quoteId));
        payment.setMercadoPagoOrderId(order.orderId());
        payment.setCheckoutUrl(order.checkoutUrl());
        payment.setOrderStatus("created");
        payment.setStatusDetail("created");
        ServicePaymentOrderAttempt attempt = new ServicePaymentOrderAttempt();
        attempt.setPagamento(payment);
        attempt.setNumeroTentativa(payment.getTentativa());
        attempt.setMercadoPagoOrderId(order.orderId());
        attempt.setIdempotencyKey(idempotencyKey.toString());
        attempt.setExternalReference(payment.getExternalReference());
        attempt.setCheckoutUrl(order.checkoutUrl());
        attempt.setOrderStatus("created");
        attempt.setStatusDetail("created");
        attemptRepository.persist(attempt);
        return toResponse(payment);
    }

    @Transactional
    public PaymentLookup buscarPorOrcamento(Integer clientId, Long quoteId) {
        ServicePayment payment = paymentRepository.buscarPorOrcamento(quoteId);
        if (payment == null || !payment.getOrcamento().getSolicitacao().getUsuario().getId().equals(clientId)) {
            return new PaymentLookup(false, null);
        }
        return new PaymentLookup(true, toResponse(payment));
    }

    @Transactional
    public PaymentResponse buscarPorId(Integer clientId, Long paymentId) {
        ServicePayment payment = paymentRepository.buscarPorId(paymentId);
        if (payment == null || !payment.getOrcamento().getSolicitacao().getUsuario().getId().equals(clientId)) {
            throw notFound();
        }
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse sincronizarComoCliente(Integer clientId, Long paymentId) {
        validarAccessToken();
        ServicePayment initialPayment = paymentRepository.buscarPorId(paymentId);
        validarPropriedade(initialPayment, clientId);
        requestRepository.buscarComBloqueio(initialPayment.getOrcamento().getSolicitacao().getId());
        ServicePayment payment = paymentRepository.buscarPorIdComBloqueio(paymentId);
        validarPropriedade(payment, clientId);
        if ((payment.getStatus() == PaymentStatus.AGUARDANDO_PAGAMENTO
                || payment.getStatus() == PaymentStatus.REEMBOLSO_PENDENTE)
                && payment.getMercadoPagoOrderId() != null) {
            sincronizar(payment);
        }
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse cancelar(Integer clientId, Long paymentId) {
        validarAccessToken();
        ServicePayment initialPayment = paymentRepository.buscarPorId(paymentId);
        validarPropriedade(initialPayment, clientId);
        requestRepository.buscarComBloqueio(initialPayment.getOrcamento().getSolicitacao().getId());
        ServicePayment payment = paymentRepository.buscarPorIdComBloqueio(paymentId);
        validarPropriedade(payment, clientId);
        if (payment.getStatus() == PaymentStatus.CANCELADO
                || payment.getStatus() == PaymentStatus.EXPIRADO) {
            return toResponse(payment);
        }
        if (payment.getStatus() != PaymentStatus.AGUARDANDO_PAGAMENTO
                || payment.getMercadoPagoOrderId() == null) {
            throw conflict("Este pagamento não pode mais ser cancelado.");
        }

        MercadoPagoOrderGateway.OrderSnapshot snapshot = orderGateway.cancelarPedido(
                accessToken,
                payment.getMercadoPagoOrderId(),
                chaveDeterministica("cancel-" + payment.getTentativa(), payment.getOrcamento().getId()));
        aplicarEstadoPedido(payment, snapshot);
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse reembolsar(Long paymentId, Integer adminId) {
        validarAccessToken();
        ServicePayment initialPayment = paymentRepository.buscarPorId(paymentId);
        if (initialPayment == null) {
            throw notFound();
        }
        requestRepository.buscarComBloqueio(initialPayment.getOrcamento().getSolicitacao().getId());
        ServicePayment payment = paymentRepository.buscarPorIdComBloqueio(paymentId);
        if (payment == null) {
            throw notFound();
        }
        if (payment.getStatus() == PaymentStatus.REEMBOLSADO) {
            return toResponse(payment);
        }
        if (payment.getStatus() != PaymentStatus.PAGO
                && payment.getStatus() != PaymentStatus.REEMBOLSO_PENDENTE) {
            throw conflict("Somente pagamentos confirmados podem ser reembolsados.");
        }
        if (payment.getMercadoPagoOrderId() == null) {
            throw conflict("O pedido do Mercado Pago não está disponível para reembolso.");
        }

        if (payment.getStatus() == PaymentStatus.PAGO) {
            alterarStatus(payment, PaymentStatus.REEMBOLSO_PENDENTE,
                    LifecycleStatusTransitionPolicy.ADMINISTRADOR, buscarUsuario(adminId));
        }
        MercadoPagoOrderGateway.OrderSnapshot snapshot = orderGateway.reembolsarPedido(
                accessToken,
                payment.getMercadoPagoOrderId(),
                chaveDeterministica("refund-" + payment.getTentativa(), payment.getOrcamento().getId()));
        aplicarEstadoPedido(payment, snapshot);
        return toResponse(payment);
    }

    @Transactional
    public ServiceCancellationResponse cancelarServico(Integer clientId, Long paymentId, String reason) {
        ServicePayment initialPayment = paymentRepository.buscarPorId(paymentId);
        if (initialPayment == null
                || !initialPayment.getOrcamento().getSolicitacao().getUsuario().getId().equals(clientId)) {
            throw notFound();
        }
        Long quoteId = initialPayment.getOrcamento().getId();
        Long requestId = initialPayment.getOrcamento().getSolicitacao().getId();

        ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
        ServicePayment payment = paymentRepository.buscarPorIdComBloqueio(paymentId);
        validarPropriedade(payment, clientId);
        if (request == null) {
            throw notFound();
        }
        ServiceCancellation cancellation = cancellationRepository.buscarPorPagamentoComBloqueio(paymentId);
        if (cancellation == null) {
            String validatedReason = ServiceCancellationPolicy.validarMotivo(reason);
            boolean executionStarted = executionStarted(quoteId);
            ServiceCancellationPolicy.validarCancelamento(payment.getStatus(), executionStarted);
            if (payment.getStatus() != PaymentStatus.PAGO) {
                throw conflict("Já existe uma operação de reembolso para este pagamento.");
            }
            if (payment.getMercadoPagoOrderId() == null) {
                throw conflict("O pedido do Mercado Pago não está disponível para reembolso.");
            }

            cancellation = new ServiceCancellation();
            cancellation.setPagamento(payment);
            cancellation.setMotivo(validatedReason);
            cancellation.setStatus(ServiceCancellationStatus.REEMBOLSO_PENDENTE);
            cancellation.setResultado("Solicitação registrada; aguardando confirmação do Mercado Pago.");
            cancellationRepository.persist(cancellation);
            cancellationRepository.flush();
            transicionar(payment, PaymentStatus.REEMBOLSO_PENDENTE,
                    LifecycleStatusTransitionPolicy.CLIENTE, request.getUsuario());
        } else {
            if (cancellation.getStatus() == ServiceCancellationStatus.REEMBOLSADO) {
                return cancellationResponse(payment, cancellation);
            }
            ServiceCancellationPolicy.validarCancelamento(payment.getStatus(), executionStarted(quoteId));
            if (payment.getStatus() == PaymentStatus.PAGO) {
                transicionar(payment, PaymentStatus.REEMBOLSO_PENDENTE,
                        LifecycleStatusTransitionPolicy.CLIENTE, request.getUsuario());
            }
        }

        validarAccessToken();
        try {
            MercadoPagoOrderGateway.OrderSnapshot snapshot = orderGateway.reembolsarPedido(
                    accessToken,
                    payment.getMercadoPagoOrderId(),
                    chaveDeterministica("service-cancel-refund", payment.getId()));
            aplicarEstadoPedido(payment, snapshot);
            if (payment.getStatus() == PaymentStatus.REEMBOLSADO) {
                cancellation.setStatus(ServiceCancellationStatus.REEMBOLSADO);
                cancellation.setResultado("Reembolso integral confirmado pelo Mercado Pago.");
                cancellation.setDataReembolso(LocalDateTime.now());
                cancelarExecucao(quoteId, request.getUsuario());
            } else if (payment.getStatus() == PaymentStatus.REEMBOLSADO_PARCIAL) {
                cancellation.setStatus(ServiceCancellationStatus.FALHA_REEMBOLSO);
                cancellation.setResultado(
                        "O Mercado Pago informou reembolso parcial; é necessária análise administrativa.");
            } else {
                cancellation.setStatus(ServiceCancellationStatus.REEMBOLSO_PENDENTE);
                cancellation.setResultado("O Mercado Pago ainda não confirmou o reembolso integral.");
            }
        } catch (WebApplicationException exception) {
            LOG.warnf("Service cancellation refund for payment %d failed with HTTP status %d.",
                    paymentId, exception.getResponse().getStatus());
            cancellation.setStatus(ServiceCancellationStatus.FALHA_REEMBOLSO);
            cancellation.setResultado("Não foi possível confirmar o reembolso. A solicitação pode ser tentada novamente.");
        }
        return cancellationResponse(payment, cancellation);
    }

    @Transactional
    public void processarWebhook(String orderId, String requestId, String signature) {
        if (!signatureVerifier.configurado()) {
            throw new WebApplicationException(
                    "Configure MERCADOPAGO_WEBHOOK_SECRET no ambiente do backend.",
                    Response.Status.SERVICE_UNAVAILABLE);
        }
        if (!signatureVerifier.validar(signature, requestId, orderId)) {
            throw new WebApplicationException("Assinatura do webhook inválida.", Response.Status.UNAUTHORIZED);
        }
        sincronizarPorOrderId(orderId);
    }

    @Transactional
    public void sincronizar(Long paymentId) {
        validarAccessToken();
        ServicePayment initialPayment = paymentRepository.buscarPorId(paymentId);
        if (initialPayment == null) {
            return;
        }
        requestRepository.buscarComBloqueio(initialPayment.getOrcamento().getSolicitacao().getId());
        ServicePayment payment = paymentRepository.buscarPorIdComBloqueio(paymentId);
        if (payment == null || payment.getMercadoPagoOrderId() == null) {
            return;
        }
        sincronizar(payment);
    }

    private void sincronizarPorOrderId(String orderId) {
        validarAccessToken();
        ServicePayment initialPayment = paymentRepository.buscarPorOrderId(orderId);
        if (initialPayment == null) {
            LOG.debugf("Ignoring Mercado Pago webhook for unknown order %s.", orderId);
            return;
        }
        requestRepository.buscarComBloqueio(initialPayment.getOrcamento().getSolicitacao().getId());
        ServicePayment payment = paymentRepository.buscarPorOrderIdComBloqueio(orderId);
        if (payment == null) {
            LOG.debugf("Ignoring Mercado Pago webhook for unknown order %s.", orderId);
            return;
        }
        sincronizar(payment);
    }

    private void sincronizar(ServicePayment payment) {
        MercadoPagoOrderGateway.OrderSnapshot snapshot =
                orderGateway.consultarPedido(accessToken, payment.getMercadoPagoOrderId());
        aplicarEstadoPedido(payment, snapshot);
    }

    private boolean executionStarted(Long quoteId) {
        return lifecycleHistoryRepository.existeAlgumStatus(
                LifecycleStatusTransitionPolicy.EXECUCAO,
                quoteId,
                java.util.List.of("EM_ANDAMENTO", "CONCLUIDA_PELO_PRESTADOR", "APROVADA", "CONTESTADA"));
    }

    private void cancelarExecucao(Long quoteId, User actor) {
        executionService.cancelarAposReembolso(quoteId, actor);
    }

    private ServiceCancellationResponse cancellationResponse(
            ServicePayment payment,
            ServiceCancellation cancellation) {
        return new ServiceCancellationResponse(
                toResponse(payment),
                cancellation.getStatus().name(),
                cancellation.getMotivo(),
                cancellation.getResultado(),
                cancellation.getDataSolicitacao(),
                cancellation.getDataReembolso());
    }

    private void aplicarEstadoPedido(
            ServicePayment payment,
            MercadoPagoOrderGateway.OrderSnapshot snapshot) {
        if (!payment.getMercadoPagoOrderId().equals(snapshot.orderId())
                || !payment.getExternalReference().equals(snapshot.externalReference())) {
            LOG.errorf("Mercado Pago order identity mismatch for local payment %d.", payment.getId());
            throw new WebApplicationException(
                    "A identidade do pedido retornada pelo Mercado Pago não confere.",
                    Response.Status.BAD_GATEWAY);
        }

        payment.setOrderStatus(snapshot.status());
        payment.setStatusDetail(snapshot.statusDetail());
        payment.setValorPago(snapshot.totalPaidAmount() == null
                ? BigDecimal.ZERO.setScale(2)
                : snapshot.totalPaidAmount().setScale(2, java.math.RoundingMode.HALF_UP));
        ServicePaymentOrderAttempt attempt =
                attemptRepository.buscarPorOrderIdComBloqueio(snapshot.orderId());
        if (attempt == null || !attempt.getPagamento().getId().equals(payment.getId())) {
            LOG.errorf("Could not find the order attempt for local payment %d.", payment.getId());
            throw new WebApplicationException(
                    "A tentativa do pedido não foi encontrada no histórico local.",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
        attempt.setOrderStatus(snapshot.status());
        attempt.setStatusDetail(snapshot.statusDetail());

        PaymentStatus next = mapStatus(snapshot);
        if (next == PaymentStatus.PAGO
                && payment.getValorPago().compareTo(payment.getValorTotal()) != 0) {
            return;
        }
        if (next == PaymentStatus.REEMBOLSADO) {
            if (payment.getStatus() == PaymentStatus.AGUARDANDO_PAGAMENTO) {
                transicionar(payment, PaymentStatus.PAGO, LifecycleStatusTransitionPolicy.MERCADO_PAGO, null);
            }
            if (payment.getStatus() == PaymentStatus.PAGO
                    || payment.getStatus() == PaymentStatus.REPASSE_PENDENTE
                    || payment.getStatus() == PaymentStatus.REPASSADO) {
                transicionar(payment, PaymentStatus.REEMBOLSO_PENDENTE,
                        LifecycleStatusTransitionPolicy.MERCADO_PAGO, null);
            }
            if (payment.getStatus() == PaymentStatus.REEMBOLSO_PENDENTE) {
                transicionar(payment, PaymentStatus.REEMBOLSADO,
                        LifecycleStatusTransitionPolicy.MERCADO_PAGO, null);
            }
        } else if (next == PaymentStatus.REEMBOLSADO_PARCIAL) {
            if (payment.getStatus() == PaymentStatus.AGUARDANDO_PAGAMENTO) {
                transicionar(payment, PaymentStatus.PAGO, LifecycleStatusTransitionPolicy.MERCADO_PAGO, null);
            }
            if (payment.getStatus() == PaymentStatus.PAGO
                    || payment.getStatus() == PaymentStatus.REEMBOLSO_PENDENTE
                    || payment.getStatus() == PaymentStatus.REPASSE_PENDENTE
                    || payment.getStatus() == PaymentStatus.REPASSADO) {
                transicionar(payment, PaymentStatus.REEMBOLSADO_PARCIAL,
                        LifecycleStatusTransitionPolicy.MERCADO_PAGO, null);
            }
        } else if (next != payment.getStatus() && podeTransicionar(payment.getStatus(), next)) {
            transicionar(payment, next, LifecycleStatusTransitionPolicy.MERCADO_PAGO, null);
        }
    }

    static PaymentStatus mapStatus(MercadoPagoOrderGateway.OrderSnapshot snapshot) {
        return switch (snapshot.status()) {
            case "processed" -> "partially_refunded".equals(snapshot.statusDetail())
                    ? PaymentStatus.REEMBOLSADO_PARCIAL
                    : PaymentStatus.PAGO;
            case "created", "action_required", "processing" -> PaymentStatus.AGUARDANDO_PAGAMENTO;
            case "failed" -> "expired".equals(snapshot.statusDetail())
                    ? PaymentStatus.EXPIRADO
                    : PaymentStatus.RECUSADO;
            case "canceled" -> "expired".equals(snapshot.statusDetail())
                    ? PaymentStatus.EXPIRADO
                    : PaymentStatus.CANCELADO;
            case "refunded" -> PaymentStatus.REEMBOLSADO;
            case "charged_back" -> PaymentStatus.CONTESTADO;
            default -> {
                LOG.warnf("Received unsupported Mercado Pago order status '%s'.", snapshot.status());
                throw new WebApplicationException(
                        "O Mercado Pago retornou um status de pedido não suportado.",
                        Response.Status.BAD_GATEWAY);
            }
        };
    }

    static boolean podeTransicionar(PaymentStatus current, PaymentStatus next) {
        return switch (current) {
            case AGUARDANDO_PAGAMENTO -> next == PaymentStatus.PAGO
                    || next == PaymentStatus.RECUSADO
                    || next == PaymentStatus.EXPIRADO
                    || next == PaymentStatus.CANCELADO
                    || next == PaymentStatus.CONTESTADO;
            case PAGO -> next == PaymentStatus.REEMBOLSO_PENDENTE
                    || next == PaymentStatus.CONTESTADO;
            case REEMBOLSO_PENDENTE -> next == PaymentStatus.REEMBOLSADO
                    || next == PaymentStatus.PAGO
                    || next == PaymentStatus.REEMBOLSADO_PARCIAL
                    || next == PaymentStatus.CONTESTADO;
            case REEMBOLSADO_PARCIAL -> next == PaymentStatus.CONTESTADO;
            case REPASSE_PENDENTE -> next == PaymentStatus.REEMBOLSO_PENDENTE
                    || next == PaymentStatus.REEMBOLSADO_PARCIAL
                    || next == PaymentStatus.CONTESTADO;
            case REPASSADO -> next == PaymentStatus.REEMBOLSO_PENDENTE
                    || next == PaymentStatus.REEMBOLSADO_PARCIAL
                    || next == PaymentStatus.CONTESTADO;
            default -> false;
        };
    }

    private void transicionar(
            ServicePayment payment,
            PaymentStatus next,
            String actorType,
            User actor) {
        alterarStatus(payment, next, actorType, actor);
        if (next == PaymentStatus.PAGO) {
            executionService.criarAguardandoSeAusente(payment.getOrcamento());
        }
        String[] message = notification(next);
        notificationService.criar(
                payment.getOrcamento().getSolicitacao().getUsuario(),
                payment.getOrcamento().getSolicitacao(),
                payment.getOrcamento(),
                message[0],
                message[1]);
    }

    private void alterarStatus(
            ServicePayment payment,
            PaymentStatus next,
            String actorType,
            User actor) {
        lifecycleStatusService.registrarTransicao(
                LifecycleStatusTransitionPolicy.PAGAMENTO,
                payment.getId(),
                payment.getStatus().name(),
                next.name(),
                actorType,
                actor);
        payment.setStatus(next);
    }

    private String[] notification(PaymentStatus status) {
        return switch (status) {
            case PAGO -> new String[] {"PAGAMENTO_CONFIRMADO", "O Mercado Pago confirmou seu pagamento."};
            case RECUSADO -> new String[] {"PAGAMENTO_RECUSADO", "O pagamento não foi aprovado. Você pode tentar novamente pelo Mercado Pago."};
            case EXPIRADO -> new String[] {"PAGAMENTO_EXPIRADO", "O prazo do pagamento expirou."};
            case CANCELADO -> new String[] {"PAGAMENTO_CANCELADO", "O pedido de pagamento foi cancelado."};
            case REEMBOLSO_PENDENTE -> new String[] {"REEMBOLSO_PENDENTE", "O reembolso do pagamento está sendo processado."};
            case REEMBOLSADO -> new String[] {"PAGAMENTO_REEMBOLSADO", "O Mercado Pago confirmou o reembolso."};
            case REEMBOLSADO_PARCIAL -> new String[] {"PAGAMENTO_REEMBOLSADO_PARCIAL", "O Mercado Pago informou um reembolso parcial."};
            case CONTESTADO -> new String[] {"PAGAMENTO_CONTESTADO", "O Mercado Pago informou uma contestação deste pagamento."};
            default -> new String[] {"PAGAMENTO_ATUALIZADO", "O status do pagamento foi atualizado."};
        };
    }

    private MercadoPagoOrderGateway.ReturnUrls returnUrls(Long quoteId) {
        if (returnUrlBase == null || returnUrlBase.isBlank()) {
            throw new WebApplicationException(
                    "Configure MERCADOPAGO_PAYMENT_RETURN_URL no ambiente do backend.",
                    Response.Status.SERVICE_UNAVAILABLE);
        }
        String base = returnUrlBase.trim();
        if (base.contains("?") || base.contains("#")) {
            throw new WebApplicationException(
                    "MERCADOPAGO_PAYMENT_RETURN_URL não pode conter query string ou fragmento.",
                    Response.Status.SERVICE_UNAVAILABLE);
        }
        String query = "?quoteId=" + quoteId;
        return new MercadoPagoOrderGateway.ReturnUrls(
                base + query + "&result=success",
                base + query + "&result=failure",
                base + query + "&result=pending");
    }

    private PaymentResponse toResponse(ServicePayment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrcamento().getId(),
                payment.getStatus().name(),
                payment.getCheckoutUrl(),
                payment.getValorTotal(),
                payment.getValorPago(),
                payment.getOrderStatus(),
                payment.getStatusDetail(),
                payment.getDataCriacao(),
                payment.getUltimaAtualizacao());
    }

    private boolean permiteNovaTentativa(PaymentStatus status) {
        return status == PaymentStatus.RECUSADO
                || status == PaymentStatus.EXPIRADO
                || status == PaymentStatus.CANCELADO;
    }

    private void validarPropriedade(ServicePayment payment, Integer clientId) {
        if (payment == null || !payment.getOrcamento().getSolicitacao().getUsuario().getId().equals(clientId)) {
            throw notFound();
        }
    }

    private User buscarUsuario(Integer userId) {
        User user = userRepository.buscarUsuarioPorId(userId);
        if (user == null) {
            throw notFound();
        }
        return user;
    }

    private void validarAccessToken() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new WebApplicationException(
                    "Configure MERCADOPAGO_ACCESS_TOKEN no ambiente do backend.",
                    Response.Status.SERVICE_UNAVAILABLE);
        }
    }

    private UUID chaveDeterministica(String operation, Long quoteId) {
        return UUID.nameUUIDFromBytes(
                ("acaba-aqui:" + operation + ":" + quoteId).getBytes(StandardCharsets.UTF_8));
    }

    private WebApplicationException notFound() {
        return new WebApplicationException("Pagamento não encontrado.", Response.Status.NOT_FOUND);
    }

    private WebApplicationException conflict(String message) {
        return new WebApplicationException(message, Response.Status.CONFLICT);
    }

    public record PaymentResponse(
            Long id,
            Long quoteId,
            String status,
            String checkoutUrl,
            BigDecimal amount,
            BigDecimal paidAmount,
            String orderStatus,
            String statusDetail,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) { }

    public record PaymentLookup(boolean exists, PaymentResponse payment) { }

    public record ServiceCancellationResponse(
            PaymentResponse payment,
            String status,
            String reason,
            String result,
            LocalDateTime requestedAt,
            LocalDateTime refundedAt) { }
}
