package com.acabaaqui.resources;

import com.acabaaqui.model.ServiceDisputeDecision;
import com.acabaaqui.services.ServiceDisputeService;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceDisputeResourceTest {

    @Test
    void onlyClientsCanOpenDisputesAndOnlyAdministratorsCanListAndDecide() {
        FakeDisputeService service = new FakeDisputeService();
        ServiceDisputeResource resource = new ServiceDisputeResource();
        resource.service = service;

        Response clientOpen = resource.abrir(
                21L,
                new ServiceDisputeResource.OpenDisputeRequest("Serviço incompleto"),
                context(10, "cliente"));
        Response providerOpen = resource.abrir(
                21L,
                new ServiceDisputeResource.OpenDisputeRequest("Motivo"),
                context(11, "prestador"));
        Response clientList = resource.listar(context(10, "cliente"));
        Response providerDecision = resource.decidir(
                5L,
                new ServiceDisputeResource.DecisionRequest(
                        ServiceDisputeDecision.PAGAR_PRESTADOR, "Análise"),
                context(11, "prestador"));
        Response adminList = resource.listar(context(1, "administrador"));
        Response adminDecision = resource.decidir(
                5L,
                new ServiceDisputeResource.DecisionRequest(
                        ServiceDisputeDecision.PAGAR_PRESTADOR, "Análise"),
                context(1, "administrador"));
        Response adminUpload = resource.anexarEvidencia(5L, null, context(1, "administrador"));
        Response unauthenticatedDownload = resource.baixarEvidencia(5L, 7L, null);

        assertEquals(Response.Status.OK.getStatusCode(), clientOpen.getStatus());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), providerOpen.getStatus());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), clientList.getStatus());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), providerDecision.getStatus());
        assertEquals(Response.Status.OK.getStatusCode(), adminList.getStatus());
        assertEquals(Response.Status.OK.getStatusCode(), adminDecision.getStatus());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), adminUpload.getStatus());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), unauthenticatedDownload.getStatus());
        assertEquals(1, service.openCalls);
        assertEquals(1, service.decisionCalls);
    }

    private SecurityContext context(int userId, String role) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> String.valueOf(userId);
            }

            @Override
            public boolean isUserInRole(String candidate) {
                return role.equals(candidate);
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

    private static class FakeDisputeService extends ServiceDisputeService {
        private int openCalls;
        private int decisionCalls;

        @Override
        public ServiceDisputeService.DisputeResponse abrir(Integer clientId, Long quoteId, String reason) {
            openCalls++;
            return response();
        }

        @Override
        public List<ServiceDisputeService.DisputeResponse> listarParaAdministrador() {
            return List.of(response());
        }

        @Override
        public ServiceDisputeService.DisputeResponse decidir(
                Long disputeId,
                Integer adminId,
                ServiceDisputeDecision decision,
                String reason) {
            decisionCalls++;
            return response();
        }

        private ServiceDisputeService.DisputeResponse response() {
            return new ServiceDisputeService.DisputeResponse(
                    5L, 21L, "Reparo", 10, "Cliente", 11, "Prestador",
                    "Motivo", "ABERTA", null, null, null, null, null, null, null, List.of());
        }
    }
}
