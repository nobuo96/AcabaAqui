package com.acabaaqui.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pagamento_tentativas", uniqueConstraints = {
        @UniqueConstraint(name = "uq_pagamento_tentativa_numero", columnNames = {"pagamento_id", "numero_tentativa"}),
        @UniqueConstraint(name = "uq_pagamento_tentativa_order", columnNames = "mercado_pago_order_id"),
        @UniqueConstraint(name = "uq_pagamento_tentativa_idempotency", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uq_pagamento_tentativa_reference", columnNames = "external_reference")
})
public class ServicePaymentOrderAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tentativa_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pagamento_id", nullable = false)
    private ServicePayment pagamento;

    @Column(name = "numero_tentativa", nullable = false)
    private int numeroTentativa;

    @Column(name = "mercado_pago_order_id", nullable = false, length = 64)
    private String mercadoPagoOrderId;

    @Column(name = "idempotency_key", nullable = false, length = 36)
    private String idempotencyKey;

    @Column(name = "external_reference", nullable = false, length = 64)
    private String externalReference;

    @Column(name = "checkout_url", nullable = false, length = 2048)
    private String checkoutUrl;

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
    public ServicePayment getPagamento() { return pagamento; }
    public void setPagamento(ServicePayment pagamento) { this.pagamento = pagamento; }
    public int getNumeroTentativa() { return numeroTentativa; }
    public void setNumeroTentativa(int numeroTentativa) { this.numeroTentativa = numeroTentativa; }
    public String getMercadoPagoOrderId() { return mercadoPagoOrderId; }
    public void setMercadoPagoOrderId(String mercadoPagoOrderId) { this.mercadoPagoOrderId = mercadoPagoOrderId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public String getStatusDetail() { return statusDetail; }
    public void setStatusDetail(String statusDetail) { this.statusDetail = statusDetail; }
}
