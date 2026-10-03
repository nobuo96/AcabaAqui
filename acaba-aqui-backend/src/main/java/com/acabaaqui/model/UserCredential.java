package com.acabaaqui.model;

import jakarta.persistence.*;

@Entity
@Table(name = "credenciais_usuario")
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credencial_id")
    private Integer id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Integer usuarioId;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "salt", nullable = false, length = 64)
    private String salt;

    @Enumerated(EnumType.STRING)
    @Column(name = "algoritmo", nullable = false, length = 10)
    private AlgoritmoCredential algoritmo;

    @Column(name = "ativo")
    private Boolean ativo = true;

    @Column(name = "versao_token", nullable = false)
    private Integer versaoToken = 0;

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
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

    public AlgoritmoCredential getAlgoritmo() {
        return algoritmo;
    }

    public void setAlgoritmo(AlgoritmoCredential algoritmo) {
        this.algoritmo = algoritmo;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Integer getVersaoToken() {
        return versaoToken;
    }

    public void setVersaoToken(Integer versaoToken) {
        this.versaoToken = versaoToken;
    }
}