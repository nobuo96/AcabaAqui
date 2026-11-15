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
@Table(name = "historico_status")
public class LifecycleStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historico_id")
    private Long id;

    @Column(name = "tipo_entidade", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entidade_id", nullable = false)
    private Long entityId;

    @Column(name = "status_anterior", length = 40)
    private String previousStatus;

    @Column(name = "status_novo", nullable = false, length = 40)
    private String newStatus;

    @Column(name = "tipo_ator", nullable = false, length = 20)
    private String actorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ator_id")
    private User actor;

    @CreationTimestamp
    @Column(name = "data_evento", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    public Long getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
