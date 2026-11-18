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
@Table(name = "chaves_pix_prestador")
public class ProviderPixKey {

    @Id
    @Column(name = "prestador_id")
    private Integer id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private User prestador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_chave", nullable = false, length = 20)
    private PixKeyType tipo;

    @Column(name = "chave_criptografada", nullable = false, length = 512)
    private String chaveCriptografada;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(name = "ultima_atualizacao", nullable = false)
    private LocalDateTime ultimaAtualizacao;

    public Integer getId() { return id; }
    public User getPrestador() { return prestador; }
    public void setPrestador(User prestador) { this.prestador = prestador; }
    public PixKeyType getTipo() { return tipo; }
    public void setTipo(PixKeyType tipo) { this.tipo = tipo; }
    public String getChaveCriptografada() { return chaveCriptografada; }
    public void setChaveCriptografada(String chaveCriptografada) { this.chaveCriptografada = chaveCriptografada; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public LocalDateTime getUltimaAtualizacao() { return ultimaAtualizacao; }
}
