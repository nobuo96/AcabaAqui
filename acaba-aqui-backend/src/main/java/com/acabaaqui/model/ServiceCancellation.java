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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cancelamentos_servico")
public class ServiceCancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cancelamento_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pagamento_id", nullable = false, unique = true)
    private ServicePayment pagamento;

    @Column(name = "motivo", nullable = false, length = 1000)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ServiceCancellationStatus status = ServiceCancellationStatus.REEMBOLSO_PENDENTE;

    @Column(name = "resultado", length = 500)
    private String resultado;

    @CreationTimestamp
    @Column(name = "data_solicitacao", nullable = false, updatable = false)
    private LocalDateTime dataSolicitacao;

    @UpdateTimestamp
    @Column(name = "ultima_atualizacao", nullable = false)
    private LocalDateTime ultimaAtualizacao;

    @Column(name = "data_reembolso")
    private LocalDateTime dataReembolso;

    public Long getId() { return id; }
    public ServicePayment getPagamento() { return pagamento; }
    public void setPagamento(ServicePayment pagamento) { this.pagamento = pagamento; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public ServiceCancellationStatus getStatus() { return status; }
    public void setStatus(ServiceCancellationStatus status) { this.status = status; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public LocalDateTime getDataSolicitacao() { return dataSolicitacao; }
    public LocalDateTime getUltimaAtualizacao() { return ultimaAtualizacao; }
    public LocalDateTime getDataReembolso() { return dataReembolso; }
    public void setDataReembolso(LocalDateTime dataReembolso) { this.dataReembolso = dataReembolso; }
}
