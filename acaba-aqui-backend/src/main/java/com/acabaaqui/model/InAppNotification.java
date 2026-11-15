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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacoes_internas")
public class InAppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificacao_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id")
    private ServiceRequest solicitacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orcamento_id")
    private ServiceQuote orcamento;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "mensagem", nullable = false, length = 255)
    private String mensagem;

    @Column(name = "lida", nullable = false)
    private boolean lida;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUsuario() { return usuario; }
    public void setUsuario(User usuario) { this.usuario = usuario; }
    public ServiceRequest getSolicitacao() { return solicitacao; }
    public void setSolicitacao(ServiceRequest solicitacao) { this.solicitacao = solicitacao; }
    public ServiceQuote getOrcamento() { return orcamento; }
    public void setOrcamento(ServiceQuote orcamento) { this.orcamento = orcamento; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public boolean isLida() { return lida; }
    public void setLida(boolean lida) { this.lida = lida; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
