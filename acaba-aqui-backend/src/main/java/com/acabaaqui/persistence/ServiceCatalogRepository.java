package com.acabaaqui.persistence;

import com.acabaaqui.model.ServiceCatalogItem;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ServiceCatalogRepository implements PanacheRepository<ServiceCatalogItem> {

    public List<ServiceCatalogItem> listar(Boolean ativo) {
        return ativo == null
                ? list("select s from ServiceCatalogItem s join fetch s.categoria order by s.nome")
                : list("select s from ServiceCatalogItem s join fetch s.categoria where s.ativo = ?1 order by s.nome", ativo);
    }
}
