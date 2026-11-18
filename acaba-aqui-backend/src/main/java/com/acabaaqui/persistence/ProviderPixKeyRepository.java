package com.acabaaqui.persistence;

import com.acabaaqui.model.ProviderPixKey;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

@ApplicationScoped
public class ProviderPixKeyRepository implements PanacheRepository<ProviderPixKey> {

    public ProviderPixKey buscarPorPrestador(Integer providerId) {
        return find("id = ?1", providerId).firstResult();
    }

    public ProviderPixKey buscarPorPrestadorComBloqueio(Integer providerId) {
        return find("id = ?1", providerId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }
}
