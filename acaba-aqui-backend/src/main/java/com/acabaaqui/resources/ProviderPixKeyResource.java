package com.acabaaqui.resources;

import com.acabaaqui.model.PixKeyType;
import com.acabaaqui.services.ServicePayoutService;
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

@Path("/provider/pix-key")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProviderPixKeyResource {

    @Inject
    ServicePayoutService service;

    @GET
    public Response buscar(@Context SecurityContext context) {
        Integer providerId = userId(context);
        return providerId == null
                ? Response.status(Response.Status.FORBIDDEN).build()
                : Response.ok(service.buscarChavePix(providerId)).build();
    }

    @PUT
    public Response atualizar(PixKeyRequest request, @Context SecurityContext context) {
        Integer providerId = userId(context);
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        if (request == null || request.type() == null || request.value() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Informe o tipo e a chave Pix."))
                    .build();
        }
        try {
            PixKeyType type = PixKeyType.valueOf(request.type());
            return Response.ok(service.atualizarChavePix(providerId, type, request.value())).build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        }
    }

    private Integer userId(SecurityContext context) {
        if (context == null || !context.isUserInRole("prestador") || context.getUserPrincipal() == null) {
            return null;
        }
        try {
            return Integer.valueOf(context.getUserPrincipal().getName());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public record PixKeyRequest(String type, String value) { }
    public record ErrorResponse(String message) { }
}
