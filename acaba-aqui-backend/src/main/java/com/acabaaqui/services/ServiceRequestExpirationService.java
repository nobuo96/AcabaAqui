package com.acabaaqui.services;

import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.persistence.ServiceRequestRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ServiceRequestExpirationService {

    private static final long DAYS_WITHOUT_RESPONSE = 7;

    @Inject
    ServiceRequestRepository requestRepository;

    @Inject
    ServiceRequestClosureService closureService;

    @Transactional
    public int expirarSemResposta(Clock clock) {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(DAYS_WITHOUT_RESPONSE);
        List<Long> requestIds = requestRepository.listarIdsCriadasAte(cutoff);
        int closed = 0;
        for (Long requestId : requestIds) {
            ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
            if (request == null || request.getStatus() != ServiceRequestStatus.CRIADA) {
                continue;
            }
            if (closureService.encerrarSeSemAceite(request)) {
                closed++;
            }
        }
        return closed;
    }
}
