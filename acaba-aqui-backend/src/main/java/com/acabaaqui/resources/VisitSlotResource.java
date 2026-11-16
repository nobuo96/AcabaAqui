package com.acabaaqui.resources;

import com.acabaaqui.model.User;
import com.acabaaqui.model.VisitSlot;
import com.acabaaqui.model.VisitSlotStatus;
import com.acabaaqui.services.VisitSlotService;
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

import java.time.LocalDateTime;
import java.util.List;

@Path("/visit-slots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VisitSlotResource {

    @Inject
    VisitSlotService service;

    @GET
    @Path("/requests/{requestId}")
    public Response listar(@PathParam("requestId") Long requestId, @Context SecurityContext context) {
        Integer userId = authenticatedUserId(context);
        boolean clientRole = context != null && context.isUserInRole("cliente");
        boolean providerRole = context != null && context.isUserInRole("prestador");
        if (userId == null || (!clientRole && !providerRole)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        List<VisitSlot> slots = service.listar(requestId, userId, clientRole, providerRole);
        return slots == null ? Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok(slots.stream().map(slot -> toResponse(slot, userId, clientRole)).toList()).build();
    }

    @POST
    @Path("/{slotId}/reserve")
    public Response reservar(@PathParam("slotId") Long slotId, @Context SecurityContext context) {
        Integer providerId = providerId(context);
        if (providerId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            VisitSlot slot = service.reservar(slotId, providerId);
            return slot == null ? Response.status(Response.Status.NOT_FOUND).build()
                    : responseForState(slot, providerId, false);
        } catch (IllegalStateException exception) {
            return conflict(exception.getMessage());
        }
    }

    @POST
    @Path("/{slotId}/confirm")
    public Response confirmar(@PathParam("slotId") Long slotId, @Context SecurityContext context) {
        Integer clientId = clientId(context);
        if (clientId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            VisitSlotService.ConfirmationResult result = service.confirmar(slotId, clientId);
            return result == null ? Response.status(Response.Status.NOT_FOUND).build()
                    : confirmationResponse(result, clientId);
        } catch (IllegalStateException exception) {
            return conflict(exception.getMessage());
        }
    }

    @POST
    @Path("/{slotId}/cancel")
    public Response cancelar(@PathParam("slotId") Long slotId, @Context SecurityContext context) {
        Integer actorId = authenticatedUserId(context);
        boolean clientRole = context != null && context.isUserInRole("cliente");
        boolean providerRole = context != null && context.isUserInRole("prestador");
        if (actorId == null || (!clientRole && !providerRole)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            VisitSlot slot = service.cancelar(slotId, actorId, clientRole, providerRole);
            return slot == null ? Response.status(Response.Status.NOT_FOUND).build()
                    : responseForState(slot, actorId, clientRole);
        } catch (IllegalStateException exception) {
            return conflict(exception.getMessage());
        }
    }

    private Response responseForState(VisitSlot slot, Integer viewerId, boolean clientRole) {
        if (slot.getStatus() == VisitSlotStatus.EXPIRADA) {
            return conflict("Este horário já expirou.");
        }
        return Response.ok(toResponse(slot, viewerId, clientRole)).build();
    }

    private Response confirmationResponse(VisitSlotService.ConfirmationResult result, Integer clientId) {
        if (result.slot().getStatus() == VisitSlotStatus.EXPIRADA) {
            return conflict("Este horário já expirou.");
        }
        return Response.ok(new VisitConfirmationResponse(
                toResponse(result.slot(), clientId, true), result.conversationId())).build();
    }

    private Response conflict(String message) {
        return Response.status(Response.Status.CONFLICT).entity(new ErrorResponse(message)).build();
    }

    private VisitSlotResponse toResponse(VisitSlot slot, Integer viewerId, boolean clientRole) {
        User provider = slot.getPrestador() != null ? slot.getPrestador() : slot.getPrestadorConfirmado();
        boolean revealProvider = provider != null
                && (clientRole || provider.getId().equals(viewerId));
        return new VisitSlotResponse(
                slot.getId(),
                slot.getInicioLocal(),
                slot.getInicioLocal().plusHours(1),
                slot.getTimeZoneId(),
                slot.getStatus(),
                revealProvider ? provider.getId() : null,
                revealProvider ? provider.getNome() : null);
    }

    private Integer clientId(SecurityContext context) {
        return context != null && context.isUserInRole("cliente") ? authenticatedUserId(context) : null;
    }

    private Integer providerId(SecurityContext context) {
        return context != null && context.isUserInRole("prestador") ? authenticatedUserId(context) : null;
    }

    private Integer authenticatedUserId(SecurityContext context) {
        if (context == null || context.getUserPrincipal() == null) {
            return null;
        }
        try {
            return Integer.valueOf(context.getUserPrincipal().getName());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public record VisitSlotResponse(
            Long id,
            LocalDateTime inicioLocal,
            LocalDateTime fimLocal,
            String timeZoneId,
            VisitSlotStatus status,
            Integer prestadorId,
            String prestadorNome) {
    }

    public record VisitConfirmationResponse(VisitSlotResponse visita, Long conversationId) {
    }

    public record ErrorResponse(String message) {
    }
}
