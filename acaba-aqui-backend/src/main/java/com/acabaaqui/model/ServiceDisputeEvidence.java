package com.acabaaqui.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "evidencias_disputa")
public class ServiceDisputeEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evidencia_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disputa_id", nullable = false)
    private ServiceDispute disputa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enviado_por", nullable = false)
    private User enviadoPor;

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "tipo_conteudo", nullable = false, length = 100)
    private String tipoConteudo;

    @Column(name = "tamanho_bytes", nullable = false)
    private long tamanhoBytes;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "conteudo", nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] conteudo;

    @CreationTimestamp
    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm;

    public Long getId() { return id; }
    public ServiceDispute getDisputa() { return disputa; }
    public void setDisputa(ServiceDispute disputa) { this.disputa = disputa; }
    public User getEnviadoPor() { return enviadoPor; }
    public void setEnviadoPor(User enviadoPor) { this.enviadoPor = enviadoPor; }
    public String getNomeArquivo() { return nomeArquivo; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }
    public String getTipoConteudo() { return tipoConteudo; }
    public void setTipoConteudo(String tipoConteudo) { this.tipoConteudo = tipoConteudo; }
    public long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }
    public byte[] getConteudo() { return conteudo; }
    public void setConteudo(byte[] conteudo) { this.conteudo = conteudo; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
