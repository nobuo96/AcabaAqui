package com.acabaaqui.persistence;

import com.acabaaqui.model.ServiceCancellation;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

@ApplicationScoped
public class ServiceCancellationRepository implements PanacheRepository<ServiceCancellation> {

    public ServiceCancellation buscarPorPagamentoComBloqueio(Long paymentId) {
        return find("pagamento.id = ?1", paymentId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }
}
