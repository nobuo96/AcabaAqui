package com.acabaaqui.services;

import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.persistence.ServiceRequestRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceRequestExpirationServiceTest {

    @Test
    void expiresAtSevenDaysButNotBeforeAndCanRunMoreThanOnce() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 12, 0);
        LocalDateTime cutoff = now.minusDays(7);
        FakeRequestRepository requests = new FakeRequestRepository();
        ServiceRequest atBoundary = request(1L);
        ServiceRequest justBefore = request(2L);
        ServiceRequest older = request(3L);
        ServiceRequest accepted = request(4L);
        requests.records.put(1L, atBoundary);
        requests.records.put(2L, justBefore);
        requests.records.put(3L, older);
        requests.records.put(4L, accepted);
        requests.createdAt.put(1L, cutoff);
        requests.createdAt.put(2L, cutoff.plusNanos(1));
        requests.createdAt.put(3L, cutoff.minusNanos(1));
        requests.createdAt.put(4L, cutoff);

        RecordingClosureService closure = new RecordingClosureService(List.of(4L));
        ServiceRequestExpirationService service = new ServiceRequestExpirationService();
        service.requestRepository = requests;
        service.closureService = closure;
        Clock clock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

        assertEquals(2, service.expirarSemResposta(clock));
        assertEquals(ServiceRequestStatus.ENCERRADA, atBoundary.getStatus());
        assertEquals(ServiceRequestStatus.CRIADA, justBefore.getStatus());
        assertEquals(ServiceRequestStatus.ENCERRADA, older.getStatus());
        assertEquals(ServiceRequestStatus.CRIADA, accepted.getStatus());
        assertEquals(0, service.expirarSemResposta(clock));
        assertEquals(List.of(1L, 3L, 4L, 4L), closure.attemptedRequestIds);
    }

    private ServiceRequest request(long id) {
        ServiceRequest request = new ServiceRequest();
        request.setId(id);
        request.setStatus(ServiceRequestStatus.CRIADA);
        return request;
    }

    private static class FakeRequestRepository extends ServiceRequestRepository {
        private final Map<Long, ServiceRequest> records = new HashMap<>();
        private final Map<Long, LocalDateTime> createdAt = new HashMap<>();

        @Override
        public List<Long> listarIdsCriadasAte(LocalDateTime cutoff) {
            return records.entrySet().stream()
                    .filter(entry -> entry.getValue().getStatus() == ServiceRequestStatus.CRIADA)
                    .filter(entry -> !createdAt.get(entry.getKey()).isAfter(cutoff))
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();
        }

        @Override
        public ServiceRequest buscarComBloqueio(Long id) {
            return records.get(id);
        }
    }

    private static class RecordingClosureService extends ServiceRequestClosureService {
        private final List<Long> acceptedRequestIds;
        private final java.util.ArrayList<Long> attemptedRequestIds = new java.util.ArrayList<>();

        RecordingClosureService(List<Long> acceptedRequestIds) {
            this.acceptedRequestIds = acceptedRequestIds;
        }

        @Override
        public boolean encerrarSeSemAceite(ServiceRequest request) {
            attemptedRequestIds.add(request.getId());
            if (acceptedRequestIds.contains(request.getId())) {
                return false;
            }
            request.setStatus(ServiceRequestStatus.ENCERRADA);
            return true;
        }
    }
}
