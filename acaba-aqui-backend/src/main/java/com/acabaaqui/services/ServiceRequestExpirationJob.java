package com.acabaaqui.services;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;

@ApplicationScoped
public class ServiceRequestExpirationJob {

    @Inject
    ServiceRequestExpirationService expirationService;

    @Scheduled(every = "15m")
    void expireUnansweredRequests() {
        expirationService.expirarSemResposta(Clock.systemDefaultZone());
    }
}
