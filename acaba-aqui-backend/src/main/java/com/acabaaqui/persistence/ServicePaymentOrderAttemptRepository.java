package com.acabaaqui.persistence;

import com.acabaaqui.model.ServicePaymentOrderAttempt;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

@ApplicationScoped
public class ServicePaymentOrderAttemptRepository implements PanacheRepository<ServicePaymentOrderAttempt> {

    public ServicePaymentOrderAttempt buscarPorOrderIdComBloqueio(String orderId) {
        return find("mercadoPagoOrderId = ?1", orderId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }
}
