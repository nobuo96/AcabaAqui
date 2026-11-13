package com.acabaaqui.resources;

import com.acabaaqui.services.ProviderServicesService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/provider/services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProviderServicesResource {

    @Inject
    ProviderServicesService service;

    @GET
    public Response listar(@Context SecurityContext context) {
        Integer providerId = providerId(context);
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        List<Integer> serviceIds = service.listar(providerId);
        return serviceIds == null ? Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok(new ServicesResponse(serviceIds)).build();
    }

    @PUT
    public Response substituir(ServiceSelectionRequest request, @Context SecurityContext context) {
        Integer providerId = providerId(context);
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            List<Integer> serviceIds = service.substituir(providerId,
                    request == null ? null : request.servicoIds());
            return serviceIds == null ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.ok(new ServicesResponse(serviceIds)).build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(exception.getMessage())).build();
        }
    }

    private Integer providerId(SecurityContext context) {
        if (context == null || context.getUserPrincipal() == null || !context.isUserInRole("prestador")) {
            return null;
        }
        try {
            return Integer.valueOf(context.getUserPrincipal().getName());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public record ServiceSelectionRequest(List<Integer> servicoIds) { }
    public record ServicesResponse(List<Integer> servicoIds) { }
    public record ErrorResponse(String message) { }
}
