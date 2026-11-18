package com.acabaaqui.resources;

import com.acabaaqui.services.ServicePayoutService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
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

@Path("/payouts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServicePayoutResource {

    @Inject
    ServicePayoutService service;

    @GET
    public Response listar(@Context SecurityContext context) {
        if (userId(context) == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(service.listarRepasses()).build();
    }

    @GET
    @Path("/{payoutId}")
    public Response buscar(
            @PathParam("payoutId") Long payoutId,
            @Context SecurityContext context) {
        if (userId(context) == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            return Response.ok(service.buscarRepasse(payoutId)).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        }
    }

    @POST
    @Path("/{payoutId}/paid")
    public Response registrarPagamento(
            @PathParam("payoutId") Long payoutId,
            ServicePayoutService.PayoutPaymentRequest request,
            @Context SecurityContext context) {
        Integer adminId = userId(context);
        if (adminId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            return Response.ok(service.registrarPagamento(payoutId, adminId, request)).build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        } catch (IllegalStateException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        } catch (WebApplicationException exception) {
            return error(exception);
        }
    }

    private Integer userId(SecurityContext context) {
        if (context == null || !context.isUserInRole("administrador") || context.getUserPrincipal() == null) {
            return null;
        }
        try {
            return Integer.valueOf(context.getUserPrincipal().getName());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Response error(WebApplicationException exception) {
        return Response.status(exception.getResponse().getStatus())
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
    }

    public record ErrorResponse(String message) { }
}
