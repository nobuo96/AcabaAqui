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
@Table(name = "solicitacao_visita_horarios")
public class VisitSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "horario_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private ServiceRequest solicitacao;

    @Column(name = "inicio_local", nullable = false)
    private LocalDateTime inicioLocal;

    @Column(name = "time_zone_id", nullable = false, length = 64)
    private String timeZoneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VisitSlotStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id")
    private User prestador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_confirmado_id")
    private User prestadorConfirmado;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceRequest getSolicitacao() {
        return solicitacao;
    }

    public void setSolicitacao(ServiceRequest solicitacao) {
        this.solicitacao = solicitacao;
    }

    public LocalDateTime getInicioLocal() {
        return inicioLocal;
    }

    public void setInicioLocal(LocalDateTime inicioLocal) {
        this.inicioLocal = inicioLocal;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public void setTimeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }

    public VisitSlotStatus getStatus() {
        return status;
    }

    public void setStatus(VisitSlotStatus status) {
        this.status = status;
    }

    public User getPrestador() {
        return prestador;
    }

    public void setPrestador(User prestador) {
        this.prestador = prestador;
    }

    public User getPrestadorConfirmado() {
        return prestadorConfirmado;
    }

    public void setPrestadorConfirmado(User prestadorConfirmado) {
        this.prestadorConfirmado = prestadorConfirmado;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
}
