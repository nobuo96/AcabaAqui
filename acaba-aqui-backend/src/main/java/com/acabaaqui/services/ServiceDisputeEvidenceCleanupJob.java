package com.acabaaqui.services;

import com.acabaaqui.persistence.ServiceDisputeRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@ApplicationScoped
public class ServiceDisputeEvidenceCleanupJob {

    @Inject
    ServiceDisputeRepository disputeRepository;

    @Scheduled(every = "24h")
    @Transactional
    void excluirEvidenciasExpiradas() {
        disputeRepository.excluirEvidenciasEncerradasAte(LocalDateTime.now().minusDays(
                ServiceDisputePolicy.RETENTION_DAYS));
    }
}
