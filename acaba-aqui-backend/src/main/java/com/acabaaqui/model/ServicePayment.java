package com.acabaaqui.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamentos")
public class ServicePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pagamento_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false, unique = true)
    private ServiceQuote orcamento;

    @Column(name = "mercado_pago_order_id", length = 64, unique = true)
    private String mercadoPagoOrderId;

    @Column(name = "idempotency_key", nullable = false, length = 36, unique = true)
    private String idempotencyKey;

    @Column(name = "external_reference", nullable = false, length = 64, unique = true)
    private String externalReference;

    @Column(name = "tentativa", nullable = false)
    private int tentativa = 1;

    @Column(name = "checkout_url", length = 2048)
    private String checkoutUrl;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "valor_pago", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorPago = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.AGUARDANDO_PAGAMENTO;

    @Column(name = "status_order", length = 40)
    private String orderStatus;

    @Column(name = "status_detail", length = 80)
    private String statusDetail;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(name = "ultima_atualizacao", nullable = false)
    private LocalDateTime ultimaAtualizacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ServiceQuote getOrcamento() { return orcamento; }
    public void setOrcamento(ServiceQuote orcamento) { this.orcamento = orcamento; }
    public String getMercadoPagoOrderId() { return mercadoPagoOrderId; }
    public void setMercadoPagoOrderId(String mercadoPagoOrderId) { this.mercadoPagoOrderId = mercadoPagoOrderId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }
    public int getTentativa() { return tentativa; }
    public void setTentativa(int tentativa) { this.tentativa = tentativa; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public String getStatusDetail() { return statusDetail; }
    public void setStatusDetail(String statusDetail) { this.statusDetail = statusDetail; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public LocalDateTime getUltimaAtualizacao() { return ultimaAtualizacao; }
}
