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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "prestador_servicos", uniqueConstraints = {
        @UniqueConstraint(name = "uq_prestador_servico", columnNames = { "prestador_id", "servico_id" })
})
public class ProviderService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prestador_servico_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private User prestador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false)
    private ServiceCatalogItem servico;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getPrestador() { return prestador; }
    public void setPrestador(User prestador) { this.prestador = prestador; }
    public ServiceCatalogItem getServico() { return servico; }
    public void setServico(ServiceCatalogItem servico) { this.servico = servico; }
}
