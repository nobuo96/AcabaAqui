package com.acabaaqui.services;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;

@ApplicationScoped
public class ServiceQuoteExpirationJob {

    @Inject
    ServiceQuoteService quoteService;

    @Scheduled(every = "15m")
    void expireQuotes() {
        quoteService.expirarVencidos(Clock.systemDefaultZone());
    }
}
