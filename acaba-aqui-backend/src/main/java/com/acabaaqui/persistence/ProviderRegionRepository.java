package com.acabaaqui.persistence;

import com.acabaaqui.model.ProviderRegion;
import com.acabaaqui.model.ProviderService;
import com.acabaaqui.model.ServiceCatalogItem;
import com.acabaaqui.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ProviderRegionRepository implements PanacheRepository<ProviderRegion> {

    public List<ProviderRegion> listarPorPrestador(User prestador) {
        return list("prestador = ?1 order by cidade, uf", prestador);
    }

    public List<User> buscarPrestadoresCorrespondentes(ServiceCatalogItem servico, String cidade, String uf) {
        return getEntityManager().createQuery(
                        "select distinct ps.prestador from ProviderService ps "
                                + "join ps.prestador p "
                                + "where ps.servico = :servico and p.perfil = :perfil and p.ativo = true "
                                + "and exists (select r.id from ProviderRegion r "
                                + "where r.prestador = ps.prestador and lower(r.cidade) = lower(:cidade) "
                                + "and upper(r.uf) = upper(:uf))",
                        User.class)
                .setParameter("servico", servico)
                .setParameter("cidade", cidade.trim())
                .setParameter("uf", uf.trim())
                .setParameter("perfil", com.acabaaqui.model.PerfilUser.prestador)
                .getResultList();
    }
}
