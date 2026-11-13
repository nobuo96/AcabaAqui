package com.acabaaqui.resources;

import com.acabaaqui.model.ServiceCategory;
import com.acabaaqui.model.ServiceCatalogItem;
import com.acabaaqui.services.ServiceCatalogService;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ServiceCatalogResourceTest {

    @Test
    void allowsProvidersToListOnlyActiveServices() {
        FakeServiceCatalogService catalog = new FakeServiceCatalogService();
        ServiceCatalogResource resource = new ServiceCatalogResource();
        resource.service = catalog;

        Response response = resource.listarServicos(null, providerContext());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(Boolean.TRUE, catalog.requestedActiveFilter);
        assertInstanceOf(List.class, response.getEntity());
        assertEquals("Instalação", ((ServiceCatalogResource.ServiceResponse)
                ((List<?>) response.getEntity()).get(0)).nome());
    }

    private SecurityContext providerContext() {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> "12";
            }

            @Override
            public boolean isUserInRole(String role) {
                return "prestador".equals(role);
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public String getAuthenticationScheme() {
                return "Bearer";
            }
        };
    }

    private static class FakeServiceCatalogService extends ServiceCatalogService {
        private Boolean requestedActiveFilter;

        @Override
        public List<ServiceCatalogItem> listarServicos(Boolean ativo) {
            requestedActiveFilter = ativo;
            ServiceCategory category = new ServiceCategory();
            category.setId(1);
            category.setNome("Reformas");

            ServiceCatalogItem service = new ServiceCatalogItem();
            service.setId(2);
            service.setNome("Instalação");
            service.setDescricao("Serviço de instalação.");
            service.setCategoria(category);
            service.setAtivo(true);
            return List.of(service);
        }
    }
}
