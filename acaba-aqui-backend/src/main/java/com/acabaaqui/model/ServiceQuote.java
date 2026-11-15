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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orcamentos")
public class ServiceQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orcamento_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private ServiceRequest solicitacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private User prestador;

    @Column(name = "descricao", nullable = false, length = 2000)
    private String descricao;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ServiceQuoteStatus status = ServiceQuoteStatus.ENVIADO;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ServiceRequest getSolicitacao() { return solicitacao; }
    public void setSolicitacao(ServiceRequest solicitacao) { this.solicitacao = solicitacao; }
    public User getPrestador() { return prestador; }
    public void setPrestador(User prestador) { this.prestador = prestador; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public ServiceQuoteStatus getStatus() { return status; }
    public void setStatus(ServiceQuoteStatus status) { this.status = status; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(LocalDateTime expiraEm) { this.expiraEm = expiraEm; }
}
