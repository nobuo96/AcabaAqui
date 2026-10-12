package com.acabaaqui.services;

import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.ServiceRequestRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceRequestServiceTest {

    @Test
    void normalizesPositiveAreaToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("12.50"), ServiceRequestService.validarArea(new BigDecimal("12.5")));
        assertEquals(new BigDecimal("12.30"), ServiceRequestService.validarArea(new BigDecimal("12.3000")));
    }

    @Test
    void rejectsNonPositiveArea() {
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestService.validarArea(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestService.validarArea(new BigDecimal("-1")));
    }

    @Test
    void rejectsAreaWithMoreThanTwoMeaningfulDecimalsOrTooManyDigits() {
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestService.validarArea(new BigDecimal("1.234")));
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestService.validarArea(new BigDecimal("100000000")));
    }

    @Test
    void cancellationClosesAnOwnedRequest() {
        ServiceRequest request = request(42, ServiceRequestStatus.CRIADA);
        ServiceRequestService service = new ServiceRequestService();
        service.requestRepository = new FakeRequestRepository(request);

        ServiceRequest cancelled = service.cancelar(7, 42L);

        assertEquals(ServiceRequestStatus.ENCERRADA, cancelled.getStatus());
    }

    @Test
    void cancellationDoesNotExposeAnotherUsersRequestAndCannotRepeat() {
        ServiceRequest request = request(42, ServiceRequestStatus.CRIADA);
        ServiceRequestService service = new ServiceRequestService();
        service.requestRepository = new FakeRequestRepository(request);

        assertNull(service.cancelar(8, 42L));
        service.cancelar(7, 42L);
        assertThrows(IllegalStateException.class, () -> service.cancelar(7, 42L));
    }

    private ServiceRequest request(long id, ServiceRequestStatus status) {
        User user = new User();
        user.setId(7);
        ServiceRequest request = new ServiceRequest();
        request.setId(id);
        request.setUsuario(user);
        request.setStatus(status);
        return request;
    }

    private static class FakeRequestRepository extends ServiceRequestRepository {
        private final ServiceRequest request;

        FakeRequestRepository(ServiceRequest request) {
            this.request = request;
        }

        @Override
        public ServiceRequest buscarComBloqueio(Long id) {
            return request.getId().equals(id) ? request : null;
        }
    }
}
