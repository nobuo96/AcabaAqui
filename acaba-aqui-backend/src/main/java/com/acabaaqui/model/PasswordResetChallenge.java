package com.acabaaqui.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "redefinicao_senha")
public class PasswordResetChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "redefinicao_id")
    private Integer id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Integer usuarioId;

    @Column(name = "codigo_hmac", nullable = false, length = 64)
    private String codigoHmac;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "ultimo_envio_em", nullable = false)
    private LocalDateTime ultimoEnvioEm;

    @Column(name = "inicio_janela_envio_em", nullable = false)
    private LocalDateTime inicioJanelaEnvioEm;

    @Column(name = "tentativas", nullable = false)
    private Integer tentativas = 0;

    @Column(name = "envios_na_janela", nullable = false)
    private Integer enviosNaJanela = 1;

    public Integer getId() {
        return id;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getCodigoHmac() {
        return codigoHmac;
    }

    public void setCodigoHmac(String codigoHmac) {
        this.codigoHmac = codigoHmac;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }

    public void setExpiraEm(LocalDateTime expiraEm) {
        this.expiraEm = expiraEm;
    }

    public LocalDateTime getUltimoEnvioEm() {
        return ultimoEnvioEm;
    }

    public void setUltimoEnvioEm(LocalDateTime ultimoEnvioEm) {
        this.ultimoEnvioEm = ultimoEnvioEm;
    }

    public LocalDateTime getInicioJanelaEnvioEm() {
        return inicioJanelaEnvioEm;
    }

    public void setInicioJanelaEnvioEm(LocalDateTime inicioJanelaEnvioEm) {
        this.inicioJanelaEnvioEm = inicioJanelaEnvioEm;
    }

    public Integer getTentativas() {
        return tentativas;
    }

    public void setTentativas(Integer tentativas) {
        this.tentativas = tentativas;
    }

    public Integer getEnviosNaJanela() {
        return enviosNaJanela;
    }

    public void setEnviosNaJanela(Integer enviosNaJanela) {
        this.enviosNaJanela = enviosNaJanela;
    }
}