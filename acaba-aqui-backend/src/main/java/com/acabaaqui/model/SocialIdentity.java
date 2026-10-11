package com.acabaaqui.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "identidades_social", uniqueConstraints = {
        @UniqueConstraint(name = "uq_identidade_social_provedor_subject", columnNames = {"provedor", "subject"})
})
public class SocialIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identidade_id")
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provedor", nullable = false, length = 20)
    private SocialAuthProvider provedor;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    public Integer getId() {
        return id;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public SocialAuthProvider getProvedor() {
        return provedor;
    }

    public void setProvedor(SocialAuthProvider provedor) {
        this.provedor = provedor;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
