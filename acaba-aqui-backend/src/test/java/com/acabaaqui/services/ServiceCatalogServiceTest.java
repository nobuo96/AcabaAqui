package com.acabaaqui.services;

import com.acabaaqui.model.ServiceCategory;
import com.acabaaqui.model.ServiceCatalogItem;
import com.acabaaqui.persistence.ServiceCategoryRepository;
import com.acabaaqui.persistence.ServiceCatalogRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceCatalogServiceTest {

    @Test
    void rejectsCreatingServiceWithInactiveCategory() {
        ServiceCategory inactiveCategory = category(2, "Pintura", false);
        ServiceCatalogService service = new ServiceCatalogService();
        service.categoryRepository = new FakeCategoryRepository(inactiveCategory);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.criarServico("Pintura interna", "Descrição", 2));

        assertEquals("Selecione uma categoria ativa.", exception.getMessage());
    }

    @Test
    void preservesExistingInactiveCategoryWhenEditingService() {
        ServiceCategory inactiveCategory = category(2, "Pintura", false);
        ServiceCatalogItem item = new ServiceCatalogItem();
        item.setId(7);
        item.setNome("Pintura antiga");
        item.setDescricao("Descrição antiga");
        item.setCategoria(inactiveCategory);
        item.setAtivo(false);

        ServiceCatalogService service = new ServiceCatalogService();
        service.serviceRepository = new FakeServiceRepository(item);

        ServiceCatalogItem updated = service.atualizarServico(7, "Pintura nova", "Descrição nova", null, null);

        assertEquals("Pintura nova", updated.getNome());
        assertEquals("Descrição nova", updated.getDescricao());
        assertEquals(inactiveCategory, updated.getCategoria());
        assertFalse(updated.isAtivo());
    }

    private ServiceCategory category(int id, String nome, boolean ativo) {
        ServiceCategory category = new ServiceCategory();
        category.setId(id);
        category.setNome(nome);
        category.setAtivo(ativo);
        return category;
    }

    private static class FakeCategoryRepository extends ServiceCategoryRepository {
        private final Map<Long, ServiceCategory> categories = new HashMap<>();

        FakeCategoryRepository(ServiceCategory... values) {
            for (ServiceCategory category : values) {
                categories.put(category.getId().longValue(), category);
            }
        }

        @Override
        public ServiceCategory findById(Long id) {
            return categories.get(id);
        }
    }

    private static class FakeServiceRepository extends ServiceCatalogRepository {
        private final Map<Long, ServiceCatalogItem> services = new HashMap<>();

        FakeServiceRepository(ServiceCatalogItem... values) {
            for (ServiceCatalogItem item : values) {
                services.put(item.getId().longValue(), item);
            }
        }

        @Override
        public ServiceCatalogItem findById(Long id) {
            return services.get(id);
        }
    }
}
