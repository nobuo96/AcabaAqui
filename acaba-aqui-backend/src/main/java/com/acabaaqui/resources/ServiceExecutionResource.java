package com.acabaaqui.resources;

import com.acabaaqui.services.ServiceExecutionService;
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

@Path("/service-executions")
@Produces(MediaType.APPLICATION_JSON)
public class ServiceExecutionResource {

    @Inject
    ServiceExecutionService service;

    @GET
    public Response listar(@Context SecurityContext context) {
        AuthenticatedUser user = authenticatedUser(context);
        if (user == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(service.listar(user.id(), user.provider())).build();
    }

    @GET
    @Path("/{quoteId}")
    public Response buscar(
            @PathParam("quoteId") Long quoteId,
            @Context SecurityContext context) {
        AuthenticatedUser user = authenticatedUser(context);
        if (user == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            return Response.ok(service.buscar(user.id(), user.provider(), quoteId)).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        }
    }

    @POST
    @Path("/{quoteId}/start")
    public Response iniciar(
            @PathParam("quoteId") Long quoteId,
            @Context SecurityContext context) {
        Integer providerId = userId(context, "prestador");
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return invoke(() -> service.iniciar(providerId, quoteId));
    }

    @POST
    @Path("/{quoteId}/complete")
    public Response concluir(
            @PathParam("quoteId") Long quoteId,
            @Context SecurityContext context) {
        Integer providerId = userId(context, "prestador");
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return invoke(() -> service.concluir(providerId, quoteId));
    }

    @POST
    @Path("/{quoteId}/approve")
    public Response aprovar(
            @PathParam("quoteId") Long quoteId,
            @Context SecurityContext context) {
        Integer clientId = userId(context, "cliente");
        if (clientId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return invoke(() -> service.aprovar(clientId, quoteId));
    }

    private Response invoke(ResponseSupplier supplier) {
        try {
            return Response.ok(supplier.get()).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        } catch (IllegalStateException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        }
    }

    private AuthenticatedUser authenticatedUser(SecurityContext context) {
        Integer providerId = userId(context, "prestador");
        if (providerId != null) {
            return new AuthenticatedUser(providerId, true);
        }
        Integer clientId = userId(context, "cliente");
        return clientId == null ? null : new AuthenticatedUser(clientId, false);
    }

    private Integer userId(SecurityContext context, String role) {
        if (context == null || !context.isUserInRole(role) || context.getUserPrincipal() == null) {
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

    private record AuthenticatedUser(Integer id, boolean provider) { }
    private interface ResponseSupplier {
        ServiceExecutionService.ExecutionResponse get();
    }
    public record ErrorResponse(String message) { }
}
