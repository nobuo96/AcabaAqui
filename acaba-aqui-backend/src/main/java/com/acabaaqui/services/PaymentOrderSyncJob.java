package com.acabaaqui.services;

import com.acabaaqui.persistence.ServicePaymentRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PaymentOrderSyncJob {

    private static final Logger LOG = Logger.getLogger(PaymentOrderSyncJob.class);

    @Inject
    ServicePaymentRepository paymentRepository;

    @Inject
    ServicePaymentService paymentService;

    @Scheduled(every = "15m")
    void sincronizarPagamentosPendentes() {
        for (Long paymentId : paymentRepository.listarIdsAguardando()) {
            try {
                paymentService.sincronizar(paymentId);
            } catch (WebApplicationException exception) {
                LOG.warnf("Could not synchronize Mercado Pago payment %d (HTTP %d).",
                        paymentId, exception.getResponse().getStatus());
            }
        }
    }
}
