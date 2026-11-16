package com.acabaaqui.resources;

import com.acabaaqui.services.ServicePaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServicePaymentResource {

    @Inject
    ServicePaymentService service;

    @POST
    @Path("/quotes/{quoteId}/checkout")
    @Blocking
    public Response iniciarCheckout(
            @PathParam("quoteId") Long quoteId,
            @Context SecurityContext context) {
        Integer clientId = userId(context, "cliente");
        if (clientId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            return Response.ok(service.iniciarCheckout(clientId, quoteId)).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        }
    }

    @GET
    @Path("/quotes/{quoteId}")
    public Response buscarPorOrcamento(
            @PathParam("quoteId") Long quoteId,
            @Context SecurityContext context) {
        Integer clientId = userId(context, "cliente");
        if (clientId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            return Response.ok(service.buscarPorOrcamento(clientId, quoteId)).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        }
    }

    @GET
    @Path("/{paymentId}")
    public Response buscarPorId(
            @PathParam("paymentId") Long paymentId,
            @Context SecurityContext context) {
        Integer clientId = userId(context, "cliente");
        if (clientId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            return Response.ok(service.buscarPorId(clientId, paymentId)).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        }
    }

    @POST
    @Path("/{paymentId}/sync")
    @Blocking
    public Response sincronizar(
            @PathParam("paymentId") Long paymentId,
            @Context SecurityContext context) {
        Integer clientId = userId(context, "cliente");
        if (clientId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            return Response.ok(service.sincronizarComoCliente(clientId, paymentId)).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        }
    }

    @POST
    @Path("/{paymentId}/cancel")
    @Blocking
    public Response cancelar(
            @PathParam("paymentId") Long paymentId,
            @Context SecurityContext context) {
        Integer clientId = userId(context, "cliente");
        if (clientId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            ServicePaymentService.PaymentResponse payment = service.cancelar(clientId, paymentId);
            if (!"CANCELADO".equals(payment.status()) && !"EXPIRADO".equals(payment.status())) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(new ErrorResponse("O Mercado Pago não confirmou o cancelamento do pedido."))
                        .build();
            }
            return Response.ok(payment).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        }
    }

    @POST
    @Path("/{paymentId}/refund")
    @Blocking
    public Response reembolsar(
            @PathParam("paymentId") Long paymentId,
            @Context SecurityContext context) {
        Integer adminId = userId(context, "administrador");
        if (adminId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            ServicePaymentService.PaymentResponse payment = service.reembolsar(paymentId, adminId);
            if (!"REEMBOLSADO".equals(payment.status())
                    && !"REEMBOLSO_PENDENTE".equals(payment.status())) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(new ErrorResponse("O Mercado Pago não confirmou o reembolso do pagamento."))
                        .build();
            }
            return Response.ok(payment).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        }
    }

    @POST
    @Path("/webhook")
    @Blocking
    public Response webhook(
            JsonNode payload,
            @QueryParam("data.id") String queryOrderId,
            @HeaderParam("x-request-id") String requestId,
            @HeaderParam("x-signature") String signature) {
        String orderId = queryOrderId;
        if ((orderId == null || orderId.isBlank()) && payload != null) {
            JsonNode dataId = payload.path("data").path("id");
            if (dataId.isValueNode()) {
                orderId = dataId.asText();
            }
        }
        if (orderId == null || orderId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Notificação sem identificador de pedido."))
                    .build();
        }
        try {
            service.processarWebhook(orderId, requestId, signature);
            return Response.ok().build();
        } catch (WebApplicationException exception) {
            return error(exception);
        }
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

    public record ErrorResponse(String message) { }
}
