package com.acabaaqui.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "cadastro_pendente_email")
public class PendingEmailRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cadastro_id")
    private Integer id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "telefone", nullable = false, length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 20)
    private PerfilUser perfil;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "salt", nullable = false, length = 64)
    private String salt;

    @Column(name = "algoritmo", nullable = false, length = 10)
    private String algoritmo;

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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public PerfilUser getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilUser perfil) {
        this.perfil = perfil;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getAlgoritmo() {
        return algoritmo;
    }

    public void setAlgoritmo(String algoritmo) {
        this.algoritmo = algoritmo;
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