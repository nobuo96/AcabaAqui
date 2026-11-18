package com.acabaaqui.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "execucoes_servico")
public class ServiceExecution {

    @Id
    @Column(name = "orcamento_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private ServiceQuote orcamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ServiceExecutionStatus status = ServiceExecutionStatus.AGUARDANDO_INICIO;

    @Column(name = "iniciado_em")
    private LocalDateTime iniciadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @Column(name = "aprovar_ate")
    private LocalDateTime aprovarAte;

    @Column(name = "aprovado_em")
    private LocalDateTime aprovadoEm;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(name = "ultima_atualizacao", nullable = false)
    private LocalDateTime ultimaAtualizacao;

    public Long getId() { return id; }
    public ServiceQuote getOrcamento() { return orcamento; }
    public void setOrcamento(ServiceQuote orcamento) { this.orcamento = orcamento; }
    public ServiceExecutionStatus getStatus() { return status; }
    public void setStatus(ServiceExecutionStatus status) { this.status = status; }
    public LocalDateTime getIniciadoEm() { return iniciadoEm; }
    public void setIniciadoEm(LocalDateTime iniciadoEm) { this.iniciadoEm = iniciadoEm; }
    public LocalDateTime getConcluidoEm() { return concluidoEm; }
    public void setConcluidoEm(LocalDateTime concluidoEm) { this.concluidoEm = concluidoEm; }
    public LocalDateTime getAprovarAte() { return aprovarAte; }
    public void setAprovarAte(LocalDateTime aprovarAte) { this.aprovarAte = aprovarAte; }
    public LocalDateTime getAprovadoEm() { return aprovadoEm; }
    public void setAprovadoEm(LocalDateTime aprovadoEm) { this.aprovadoEm = aprovadoEm; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public LocalDateTime getUltimaAtualizacao() { return ultimaAtualizacao; }
}
