package com.acabaaqui.resources;

import com.acabaaqui.model.ServiceQuote;
import com.acabaaqui.model.ServiceQuoteStatus;
import com.acabaaqui.services.ServiceQuoteService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Path("/service-quotes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceQuoteResource {

    @Inject
    ServiceQuoteService service;

    @POST
    @Path("/requests/{requestId}")
    public Response enviar(@PathParam("requestId") Long requestId, QuoteSubmission request,
                           @Context SecurityContext context) {
        Integer providerId = providerId(context);
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            ServiceQuote quote = service.enviar(providerId, requestId,
                    request == null ? null : new ServiceQuoteService.QuoteSubmission(
                            request.descricao(), request.valorTotal()));
            return quote == null ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.status(Response.Status.CREATED).entity(toResponse(quote)).build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(exception.getMessage())).build();
        } catch (IllegalStateException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(exception.getMessage())).build();
        }
    }

    @GET
    @Path("/requests/{requestId}")
    public Response listar(@PathParam("requestId") Long requestId, @Context SecurityContext context) {
        Integer userId = userId(context);
        boolean clientRole = context != null && context.isUserInRole("cliente");
        boolean providerRole = context != null && context.isUserInRole("prestador");
        if (userId == null || (!clientRole && !providerRole)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        var quotes = service.listar(requestId, userId, clientRole, providerRole);
        return quotes == null ? Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok(quotes.stream().map(this::toResponse).toList()).build();
    }

    @POST
    @Path("/{quoteId}/approve")
    public Response aprovar(@PathParam("quoteId") Long quoteId, @Context SecurityContext context) {
        Integer clientId = clientId(context);
        if (clientId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return decidir(() -> service.aprovar(quoteId, clientId));
    }

    @POST
    @Path("/{quoteId}/reject")
    public Response recusar(@PathParam("quoteId") Long quoteId, @Context SecurityContext context) {
        Integer clientId = clientId(context);
        if (clientId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return decidir(() -> service.recusar(quoteId, clientId));
    }

    private Response decidir(java.util.function.Supplier<ServiceQuote> decision) {
        try {
            ServiceQuote quote = decision.get();
            if (quote == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (quote.getStatus() == ServiceQuoteStatus.EXPIRADO) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(new ErrorResponse("Este orçamento expirou.")).build();
            }
            return Response.ok(toResponse(quote)).build();
        } catch (IllegalStateException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(exception.getMessage())).build();
        }
    }

    private Integer clientId(SecurityContext context) {
        return context != null && context.isUserInRole("cliente") ? userId(context) : null;
    }

    private Integer providerId(SecurityContext context) {
        return context != null && context.isUserInRole("prestador") ? userId(context) : null;
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

    private QuoteResponse toResponse(ServiceQuote quote) {
        return new QuoteResponse(
                quote.getId(),
                quote.getSolicitacao().getId(),
                quote.getPrestador().getId(),
                quote.getPrestador().getNome(),
                quote.getDescricao(),
                quote.getValorTotal(),
                quote.getStatus().name(),
                quote.getDataCriacao(),
                quote.getExpiraEm());
    }

    public record QuoteSubmission(String descricao, BigDecimal valorTotal) { }
    public record QuoteResponse(
            Long id,
            Long solicitacaoId,
            Integer prestadorId,
            String prestadorNome,
            String descricao,
            BigDecimal valorTotal,
            String status,
            LocalDateTime dataCriacao,
            LocalDateTime expiraEm) { }
    public record ErrorResponse(String message) { }
}
