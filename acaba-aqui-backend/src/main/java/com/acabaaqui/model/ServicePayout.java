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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "repasses")
public class ServicePayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repasse_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false, unique = true)
    private ServiceExecution execucao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ServicePayoutStatus status = ServicePayoutStatus.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_elegibilidade", nullable = false, length = 30)
    private ServicePayoutReason motivoElegibilidade;

    @Column(name = "elegivel_em", nullable = false)
    private LocalDateTime elegivelEm;

    @Column(name = "valor_pago", precision = 12, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "destinatario_nome", length = 100)
    private String destinatarioNome;

    @Enumerated(EnumType.STRING)
    @Column(name = "destinatario_tipo_chave", length = 20)
    private PixKeyType destinatarioTipoChave;

    @Column(name = "destinatario_chave_criptografada", length = 512)
    private String destinatarioChaveCriptografada;

    @Column(name = "referencia_transferencia", length = 500, unique = true)
    private String referenciaTransferencia;

    @Column(name = "referencia_comprovante", length = 1000)
    private String referenciaComprovante;

    @Column(name = "pago_em")
    private LocalDateTime pagoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_id")
    private User administrador;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(name = "ultima_atualizacao", nullable = false)
    private LocalDateTime ultimaAtualizacao;

    public Long getId() { return id; }
    public ServiceExecution getExecucao() { return execucao; }
    public void setExecucao(ServiceExecution execucao) { this.execucao = execucao; }
    public ServicePayoutStatus getStatus() { return status; }
    public void setStatus(ServicePayoutStatus status) { this.status = status; }
    public ServicePayoutReason getMotivoElegibilidade() { return motivoElegibilidade; }
    public void setMotivoElegibilidade(ServicePayoutReason motivoElegibilidade) { this.motivoElegibilidade = motivoElegibilidade; }
    public LocalDateTime getElegivelEm() { return elegivelEm; }
    public void setElegivelEm(LocalDateTime elegivelEm) { this.elegivelEm = elegivelEm; }
    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }
    public String getDestinatarioNome() { return destinatarioNome; }
    public void setDestinatarioNome(String destinatarioNome) { this.destinatarioNome = destinatarioNome; }
    public PixKeyType getDestinatarioTipoChave() { return destinatarioTipoChave; }
    public void setDestinatarioTipoChave(PixKeyType destinatarioTipoChave) { this.destinatarioTipoChave = destinatarioTipoChave; }
    public String getDestinatarioChaveCriptografada() { return destinatarioChaveCriptografada; }
    public void setDestinatarioChaveCriptografada(String destinatarioChaveCriptografada) { this.destinatarioChaveCriptografada = destinatarioChaveCriptografada; }
    public String getReferenciaTransferencia() { return referenciaTransferencia; }
    public void setReferenciaTransferencia(String referenciaTransferencia) { this.referenciaTransferencia = referenciaTransferencia; }
    public String getReferenciaComprovante() { return referenciaComprovante; }
    public void setReferenciaComprovante(String referenciaComprovante) { this.referenciaComprovante = referenciaComprovante; }
    public LocalDateTime getPagoEm() { return pagoEm; }
    public void setPagoEm(LocalDateTime pagoEm) { this.pagoEm = pagoEm; }
    public User getAdministrador() { return administrador; }
    public void setAdministrador(User administrador) { this.administrador = administrador; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public LocalDateTime getUltimaAtualizacao() { return ultimaAtualizacao; }
}
