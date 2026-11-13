package com.acabaaqui.services;

import com.acabaaqui.model.PerfilUser;
import com.acabaaqui.model.ServiceCatalogItem;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.ProviderServiceRepository;
import com.acabaaqui.persistence.ServiceCatalogRepository;
import com.acabaaqui.persistence.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProviderServicesServiceTest {

    @Test
    void replacesSelectedServicesAsOneDeduplicatedSet() {
        FakeProviderServiceRepository providerServices = new FakeProviderServiceRepository();
        ProviderServicesService service = service(providerServices, activeService(3), activeService(8));

        List<Integer> selected = service.substituir(12, List.of(3, 8, 3));

        assertEquals(List.of(3, 8), selected);
        assertEquals(List.of(3, 8), providerServices.selectedServiceIds);
    }

    @Test
    void rejectsUnavailableServicesWithoutReplacingCurrentSelection() {
        FakeProviderServiceRepository providerServices = new FakeProviderServiceRepository();
        ProviderServicesService service = service(providerServices, activeService(3), inactiveService(8));

        assertThrows(IllegalArgumentException.class, () -> service.substituir(12, List.of(3, 8)));

        assertEquals(List.of(), providerServices.selectedServiceIds);
    }

    @Test
    void allowsClearingAllServices() {
        FakeProviderServiceRepository providerServices = new FakeProviderServiceRepository();
        ProviderServicesService service = service(providerServices);

        assertEquals(List.of(), service.substituir(12, List.of()));
        assertEquals(List.of(), providerServices.selectedServiceIds);
    }

    private ProviderServicesService service(FakeProviderServiceRepository providerServices,
                                             ServiceCatalogItem... catalogItems) {
        User provider = new User();
        provider.setId(12);
        provider.setPerfil(PerfilUser.prestador);

        ProviderServicesService service = new ProviderServicesService();
        service.providerServiceRepository = providerServices;
        service.serviceRepository = new FakeServiceCatalogRepository(catalogItems);
        service.userRepository = new FakeUserRepository(provider);
        return service;
    }

    private ServiceCatalogItem activeService(int id) {
        ServiceCatalogItem service = new ServiceCatalogItem();
        service.setId(id);
        service.setAtivo(true);
        return service;
    }

    private ServiceCatalogItem inactiveService(int id) {
        ServiceCatalogItem service = activeService(id);
        service.setAtivo(false);
        return service;
    }

    private static class FakeProviderServiceRepository extends ProviderServiceRepository {
        private List<Integer> selectedServiceIds = List.of();

        @Override
        public void substituir(User provider, List<ServiceCatalogItem> services) {
            selectedServiceIds = services.stream().map(ServiceCatalogItem::getId).toList();
        }
    }

    private static class FakeServiceCatalogRepository extends ServiceCatalogRepository {
        private final Map<Long, ServiceCatalogItem> services = new HashMap<>();

        FakeServiceCatalogRepository(ServiceCatalogItem... values) {
            for (ServiceCatalogItem service : values) {
                services.put(service.getId().longValue(), service);
            }
        }

        @Override
        public ServiceCatalogItem findById(Long id) {
            return services.get(id);
        }
    }

    private static class FakeUserRepository extends UserRepository {
        private final User provider;

        FakeUserRepository(User provider) {
            this.provider = provider;
        }

        @Override
        public User buscarUsuarioPorId(Integer id) {
            return provider.getId().equals(id) ? provider : null;
        }
    }
}
