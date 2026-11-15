package com.acabaaqui.resources;

import com.acabaaqui.model.RequestOpportunity;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.services.RequestOpportunityService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Path("/provider/opportunities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequestOpportunityResource {

    @Inject
    RequestOpportunityService service;

    @GET
    public Response listar(@Context SecurityContext context) {
        Integer providerId = providerId(context);
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        var opportunities = service.listar(providerId);
        return opportunities == null ? Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok(opportunities.stream().map(this::toResponse).toList()).build();
    }

    @PUT
    @Path("/read")
    public Response marcarLidas(@Context SecurityContext context) {
        Integer providerId = providerId(context);
        return providerId == null ? Response.status(Response.Status.FORBIDDEN).build()
                : Response.ok(new ReadResponse(service.marcarLidas(providerId))).build();
    }

    @POST
    @Path("/{opportunityId}/accept")
    public Response aceitar(@PathParam("opportunityId") Long opportunityId,
                            @Context SecurityContext context) {
        Integer providerId = providerId(context);
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            RequestOpportunity opportunity = service.aceitar(providerId, opportunityId);
            return opportunity == null ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.noContent().build();
        } catch (IllegalStateException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(exception.getMessage())).build();
        }
    }

    @POST
    @Path("/{opportunityId}/decline")
    public Response recusar(@PathParam("opportunityId") Long opportunityId,
                            @Context SecurityContext context) {
        Integer providerId = providerId(context);
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            RequestOpportunity opportunity = service.recusar(providerId, opportunityId);
            return opportunity == null ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.noContent().build();
        } catch (IllegalStateException exception) {
            return Response.status(Response.Status.CONFLICT)
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

    private OpportunityResponse toResponse(RequestOpportunity opportunity) {
        ServiceRequest request = opportunity.getSolicitacao();
        return new OpportunityResponse(opportunity.getId(), request.getId(), request.getServico().getNome(),
                request.getAreaM2(), request.getEnderecoCidade(), request.getEnderecoUf(),
                opportunity.getStatus().name(), opportunity.isLida(), opportunity.getDataCriacao());
    }

    public record OpportunityResponse(Long id, Long solicitacaoId, String servicoNome, BigDecimal areaM2,
                                      String cidade, String uf, String status, boolean lida,
                                      LocalDateTime dataCriacao) { }
    public record ReadResponse(int atualizadas) { }
    public record ErrorResponse(String message) { }
}
