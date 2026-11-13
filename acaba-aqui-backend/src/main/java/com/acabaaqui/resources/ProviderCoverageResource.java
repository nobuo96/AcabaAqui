package com.acabaaqui.resources;

import com.acabaaqui.model.ProviderRegion;
import com.acabaaqui.services.ProviderCoverageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/provider/coverage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProviderCoverageResource {

    @Inject
    ProviderCoverageService service;

    @GET
    public Response listar(@Context SecurityContext context) {
        Integer providerId = providerId(context);
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        List<ProviderRegion> areas = service.listar(providerId);
        return areas == null ? Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok(areas.stream().map(this::toResponse).toList()).build();
    }

    @POST
    public Response criar(ProviderCoverageService.CoverageRequest request,
                          @Context SecurityContext context) {
        Integer providerId = providerId(context);
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            ProviderRegion area = service.criar(providerId, request);
            return area == null ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.status(Response.Status.CREATED).entity(toResponse(area)).build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(exception.getMessage())).build();
        }
    }

    @DELETE
    @Path("/{areaId}")
    public Response remover(@PathParam("areaId") Long areaId, @Context SecurityContext context) {
        Integer providerId = providerId(context);
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return service.remover(providerId, areaId) ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build();
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

    private CoverageResponse toResponse(ProviderRegion area) {
        return new CoverageResponse(area.getId(), area.getCidade(), area.getUf());
    }

    public record CoverageResponse(Long id, String cidade, String uf) { }
    public record ErrorResponse(String message) { }
}
