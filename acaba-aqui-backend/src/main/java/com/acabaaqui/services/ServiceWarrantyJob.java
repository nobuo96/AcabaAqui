package com.acabaaqui.services;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;

import java.time.Clock;

@ApplicationScoped
public class ServiceWarrantyJob {

    private static final Logger LOG = Logger.getLogger(ServiceWarrantyJob.class);

    @Inject
    ServiceWarrantyService warrantyService;

    @Scheduled(every = "15m")
    void processarGarantias() {
        warrantyService.marcarChamadosVencidos(Clock.systemDefaultZone());
        for (Long warrantyId : warrantyService.listarReembolsosPendentes()) {
            try {
                warrantyService.tentarReembolso(warrantyId);
            } catch (WebApplicationException exception) {
                LOG.warnf("Could not process warranty refund %d (HTTP %d).",
                        warrantyId, exception.getResponse().getStatus());
            } catch (IllegalStateException exception) {
                LOG.errorf(exception, "Could not apply warranty refund state for warranty %d.", warrantyId);
            }
        }
    }
}
