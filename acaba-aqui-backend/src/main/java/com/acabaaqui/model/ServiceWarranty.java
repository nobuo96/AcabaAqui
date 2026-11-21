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

import java.time.LocalDateTime;

@Entity
@Table(name = "garantias_servico")
public class ServiceWarranty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "garantia_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private ServiceQuote orcamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ServiceWarrantyStatus status = ServiceWarrantyStatus.ATIVA;

    @Column(name = "iniciada_em", nullable = false)
    private LocalDateTime iniciadaEm;

    @Column(name = "termina_em", nullable = false)
    private LocalDateTime terminaEm;

    @Column(name = "motivo_chamado", length = 1000)
    private String motivoChamado;

    @Column(name = "acionada_em")
    private LocalDateTime acionadaEm;

    @Column(name = "correcao_ate")
    private LocalDateTime correcaoAte;

    @Column(name = "corrigida_em")
    private LocalDateTime corrigidaEm;

    @Column(name = "resolvida_em")
    private LocalDateTime resolvidaEm;

    @Column(name = "reembolsada_em")
    private LocalDateTime reembolsadaEm;

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
    public ServiceWarrantyStatus getStatus() { return status; }
    public void setStatus(ServiceWarrantyStatus status) { this.status = status; }
    public LocalDateTime getIniciadaEm() { return iniciadaEm; }
    public void setIniciadaEm(LocalDateTime iniciadaEm) { this.iniciadaEm = iniciadaEm; }
    public LocalDateTime getTerminaEm() { return terminaEm; }
    public void setTerminaEm(LocalDateTime terminaEm) { this.terminaEm = terminaEm; }
    public String getMotivoChamado() { return motivoChamado; }
    public void setMotivoChamado(String motivoChamado) { this.motivoChamado = motivoChamado; }
    public LocalDateTime getAcionadaEm() { return acionadaEm; }
    public void setAcionadaEm(LocalDateTime acionadaEm) { this.acionadaEm = acionadaEm; }
    public LocalDateTime getCorrecaoAte() { return correcaoAte; }
    public void setCorrecaoAte(LocalDateTime correcaoAte) { this.correcaoAte = correcaoAte; }
    public LocalDateTime getCorrigidaEm() { return corrigidaEm; }
    public void setCorrigidaEm(LocalDateTime corrigidaEm) { this.corrigidaEm = corrigidaEm; }
    public LocalDateTime getResolvidaEm() { return resolvidaEm; }
    public void setResolvidaEm(LocalDateTime resolvidaEm) { this.resolvidaEm = resolvidaEm; }
    public LocalDateTime getReembolsadaEm() { return reembolsadaEm; }
    public void setReembolsadaEm(LocalDateTime reembolsadaEm) { this.reembolsadaEm = reembolsadaEm; }
}
