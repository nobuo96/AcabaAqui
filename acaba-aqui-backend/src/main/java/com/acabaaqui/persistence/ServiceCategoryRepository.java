package com.acabaaqui.persistence;

import com.acabaaqui.model.ServiceCategory;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ServiceCategoryRepository implements PanacheRepository<ServiceCategory> {

    public List<ServiceCategory> listar(Boolean ativo) {
        return ativo == null
                ? list("order by nome")
                : list("ativo = ?1 order by nome", ativo);
    }

    public ServiceCategory buscarPorNome(String nome) {
        return find("lower(nome)", nome.toLowerCase()).firstResult();
    }
}
