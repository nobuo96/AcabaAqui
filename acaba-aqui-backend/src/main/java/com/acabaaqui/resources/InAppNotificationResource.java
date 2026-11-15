package com.acabaaqui.resources;

import com.acabaaqui.model.InAppNotification;
import com.acabaaqui.services.InAppNotificationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.time.LocalDateTime;

@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
public class InAppNotificationResource {

    @Inject
    InAppNotificationService service;

    @GET
    public Response listar(@Context SecurityContext context) {
        Integer userId = userId(context);
        if (userId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(service.listar(userId).stream().map(this::toResponse).toList()).build();
    }

    @PUT
    @Path("/{notificationId}/read")
    public Response marcarLida(@PathParam("notificationId") Long notificationId,
                               @Context SecurityContext context) {
        Integer userId = userId(context);
        if (userId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return service.marcarLida(notificationId, userId)
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build();
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

    private NotificationResponse toResponse(InAppNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTipo(),
                notification.getMensagem(),
                notification.getSolicitacao() == null ? null : notification.getSolicitacao().getId(),
                notification.getOrcamento() == null ? null : notification.getOrcamento().getId(),
                notification.isLida(),
                notification.getDataCriacao());
    }

    public record NotificationResponse(
            Long id,
            String tipo,
            String mensagem,
            Long solicitacaoId,
            Long orcamentoId,
            boolean lida,
            LocalDateTime dataCriacao) { }
}
