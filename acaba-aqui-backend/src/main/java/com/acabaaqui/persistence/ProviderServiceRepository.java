package com.acabaaqui.persistence;

import com.acabaaqui.model.ProviderService;
import com.acabaaqui.model.ServiceCatalogItem;
import com.acabaaqui.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ProviderServiceRepository implements PanacheRepository<ProviderService> {

    public List<ProviderService> listarPorPrestador(User prestador) {
        return list("select ps from ProviderService ps join fetch ps.servico "
                + "where ps.prestador = ?1 and ps.servico.ativo = true order by ps.servico.nome", prestador);
    }

    public void substituir(User prestador, List<ServiceCatalogItem> servicos) {
        delete("prestador = ?1", prestador);
        for (ServiceCatalogItem servico : servicos) {
            ProviderService providerService = new ProviderService();
            providerService.setPrestador(prestador);
            providerService.setServico(servico);
            persist(providerService);
        }
    }
}
