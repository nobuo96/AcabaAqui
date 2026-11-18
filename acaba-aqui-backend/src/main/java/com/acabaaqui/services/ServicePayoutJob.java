package com.acabaaqui.services;

import com.acabaaqui.persistence.ServiceExecutionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;
import io.quarkus.scheduler.Scheduled;

import java.time.LocalDateTime;

@ApplicationScoped
public class ServicePayoutJob {

    private static final Logger LOG = Logger.getLogger(ServicePayoutJob.class);

    @Inject
    ServiceExecutionRepository executionRepository;

    @Inject
    ServicePayoutService payoutService;

    @Scheduled(every = "15m")
    void enfileirarRepassesExpirados() {
        LocalDateTime now = LocalDateTime.now();
        for (Long quoteId : executionRepository.listarConcluidasAte(now)) {
            try {
                payoutService.enfileirarAposPrazo(quoteId, now);
            } catch (WebApplicationException exception) {
                LOG.warnf("Could not enqueue service payout for quote %d (HTTP %d).",
                        quoteId, exception.getResponse().getStatus());
            }
        }
    }
}
