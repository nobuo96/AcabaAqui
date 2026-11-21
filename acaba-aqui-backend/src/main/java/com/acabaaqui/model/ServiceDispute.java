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

import java.time.LocalDateTime;

@Entity
@Table(name = "disputas_servico")
public class ServiceDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disputa_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false, unique = true)
    private ServiceQuote orcamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private User cliente;

    @Column(name = "motivo", nullable = false, length = 1000)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ServiceDisputeStatus status = ServiceDisputeStatus.ABERTA;

    @CreationTimestamp
    @Column(name = "aberta_em", nullable = false, updatable = false)
    private LocalDateTime abertaEm;

    @Column(name = "evidencias_ate", nullable = false)
    private LocalDateTime evidenciasAte;

    @Enumerated(EnumType.STRING)
    @Column(name = "decisao", length = 30)
    private ServiceDisputeDecision decisao;

    @Column(name = "justificativa_decisao", length = 1000)
    private String justificativaDecisao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_id")
    private User administrador;

    @Column(name = "encerrada_em")
    private LocalDateTime encerradaEm;

    public Long getId() { return id; }
    public ServiceQuote getOrcamento() { return orcamento; }
    public void setOrcamento(ServiceQuote orcamento) { this.orcamento = orcamento; }
    public User getCliente() { return cliente; }
    public void setCliente(User cliente) { this.cliente = cliente; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public ServiceDisputeStatus getStatus() { return status; }
    public void setStatus(ServiceDisputeStatus status) { this.status = status; }
    public LocalDateTime getAbertaEm() { return abertaEm; }
    public LocalDateTime getEvidenciasAte() { return evidenciasAte; }
    public void setEvidenciasAte(LocalDateTime evidenciasAte) { this.evidenciasAte = evidenciasAte; }
    public ServiceDisputeDecision getDecisao() { return decisao; }
    public void setDecisao(ServiceDisputeDecision decisao) { this.decisao = decisao; }
    public String getJustificativaDecisao() { return justificativaDecisao; }
    public void setJustificativaDecisao(String justificativaDecisao) { this.justificativaDecisao = justificativaDecisao; }
    public User getAdministrador() { return administrador; }
    public void setAdministrador(User administrador) { this.administrador = administrador; }
    public LocalDateTime getEncerradaEm() { return encerradaEm; }
    public void setEncerradaEm(LocalDateTime encerradaEm) { this.encerradaEm = encerradaEm; }
}
