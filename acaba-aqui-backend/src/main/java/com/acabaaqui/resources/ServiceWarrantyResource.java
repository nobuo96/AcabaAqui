package com.acabaaqui.resources;

import com.acabaaqui.services.ServiceWarrantyService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.time.Clock;

@Path("/service-warranties")
@Produces(MediaType.APPLICATION_JSON)
public class ServiceWarrantyResource {

    @Inject
    ServiceWarrantyService service;

    @GET
    @Path("/quotes/{quoteId}")
    public Response buscar(
            @PathParam("quoteId") Long quoteId,
            @Context SecurityContext context) {
        Integer userId = userId(context);
        if (userId == null || (!context.isUserInRole("cliente") && !context.isUserInRole("prestador"))) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            ServiceWarrantyService.WarrantyResponse warranty =
                    service.buscarPorOrcamento(quoteId, userId);
            return warranty == null ? Response.noContent().build() : Response.ok(warranty).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        }
    }

    @POST
    @Path("/quotes/{quoteId}")
    @jakarta.ws.rs.Consumes(MediaType.APPLICATION_JSON)
    public Response abrir(
            @PathParam("quoteId") Long quoteId,
            OpenWarrantyRequest request,
            @Context SecurityContext context) {
        Integer clientId = userId(context, "cliente");
        if (clientId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            return Response.ok(service.abrir(
                    clientId,
                    quoteId,
                    request == null ? null : request.reason(),
                    Clock.systemDefaultZone())).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(exception.getMessage())).build();
        } catch (IllegalStateException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(exception.getMessage())).build();
        }
    }

    @POST
    @Path("/{warrantyId}/corrected")
    public Response corrigir(
            @PathParam("warrantyId") Long warrantyId,
            @Context SecurityContext context) {
        Integer providerId = userId(context, "prestador");
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return invoke(() -> service.corrigir(providerId, warrantyId, Clock.systemDefaultZone()));
    }

    @POST
    @Path("/{warrantyId}/resolve")
    public Response resolver(
            @PathParam("warrantyId") Long warrantyId,
            @Context SecurityContext context) {
        Integer clientId = userId(context, "cliente");
        if (clientId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return invoke(() -> service.resolver(clientId, warrantyId, Clock.systemDefaultZone()));
    }

    private Response invoke(ResponseSupplier supplier) {
        try {
            return Response.ok(supplier.get()).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(exception.getMessage())).build();
        } catch (IllegalStateException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(exception.getMessage())).build();
        }
    }

    private Integer userId(SecurityContext context) {
        if (context == null || context.getUserPrincipal() == null) {
            return null;
        }
        try {
            return Integer.valueOf(context.getUserPrincipal().getName());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer userId(SecurityContext context, String role) {
        return context != null && context.isUserInRole(role) ? userId(context) : null;
    }

    private Response error(WebApplicationException exception) {
        return Response.status(exception.getResponse().getStatus())
                .entity(new ErrorResponse(exception.getMessage())).build();
    }

    private interface ResponseSupplier {
        ServiceWarrantyService.WarrantyResponse get();
    }

    public record OpenWarrantyRequest(String reason) { }
    public record ErrorResponse(String message) { }
}
