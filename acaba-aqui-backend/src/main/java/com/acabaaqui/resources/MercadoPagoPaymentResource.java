package com.acabaaqui.resources;

import com.acabaaqui.services.MercadoPagoPaymentService;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MercadoPagoPaymentResource {

    @Inject
    MercadoPagoPaymentService service;

    @GET
    @Path("/integration-check")
    @Blocking
    public Response verificarConexao(@Context SecurityContext securityContext) {
        if (securityContext == null || !securityContext.isUserInRole("administrador")) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            return Response.ok(service.verificarConexao()).build();
        } catch (WebApplicationException exception) {
            return Response.status(exception.getResponse().getStatus())
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/orders/sandbox-test")
    @Blocking
    public Response criarPedidoSandbox(
            SandboxOrderRequest request,
            @HeaderParam("X-Idempotency-Key") String idempotencyKey,
            @Context SecurityContext securityContext) {
        if (securityContext == null || !securityContext.isUserInRole("administrador")) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Informe a descrição e o valor do pedido."))
                    .build();
        }
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(service.criarPedidoSandbox(
                            request.description(),
                            request.amount(),
                            idempotencyKey))
                    .build();
        } catch (WebApplicationException exception) {
            return Response.status(exception.getResponse().getStatus())
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        }
    }

    public record SandboxOrderRequest(String description, java.math.BigDecimal amount) { }

    public record ErrorResponse(String message) { }
}
