package com.acabaaqui.resources;

import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.services.ServiceRequestService;
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

@Path("/user/{userId}/service-requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceRequestResource {

    @Inject
    ServiceRequestService service;

    @GET
    public Response listar(@PathParam("userId") Integer userId, @Context SecurityContext securityContext) {
        if (!isAuthorizedClient(userId, securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        var requests = service.listar(userId);
        return requests == null
                ? Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse("Usuário não encontrado.")).build()
                : Response.ok(requests.stream().map(this::toResponse).toList()).build();
    }

    @POST
    public Response criar(@PathParam("userId") Integer userId, CreateRequest request,
                          @Context SecurityContext securityContext) {
        if (!isAuthorizedClient(userId, securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            ServiceRequest created = service.criar(userId, request == null ? null
                    : new ServiceRequestService.CreateServiceRequest(
                            request.servicoId(), request.enderecoId(), request.areaM2()));
            return created == null
                    ? Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Usuário não encontrado.")).build()
                    : Response.status(Response.Status.CREATED).entity(toResponse(created)).build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(exception.getMessage())).build();
        }
    }

    @POST
    @Path("/{requestId}/cancel")
    public Response cancelar(@PathParam("userId") Integer userId, @PathParam("requestId") Long requestId,
                             @Context SecurityContext securityContext) {
        if (!isAuthorizedClient(userId, securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            ServiceRequest request = service.cancelar(userId, requestId);
            return request == null
                    ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.ok(Response.Status.OK).build();
        } catch (IllegalStateException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(exception.getMessage())).build();
        }
    }

    private boolean isAuthorizedClient(Integer userId, SecurityContext securityContext) {
        return userId != null && securityContext != null && securityContext.getUserPrincipal() != null
                && securityContext.getUserPrincipal().getName().equals(userId.toString())
                && securityContext.isUserInRole("cliente");
    }

    private ServiceRequestResponse toResponse(ServiceRequest request) {
        return new ServiceRequestResponse(
                request.getId(),
                request.getServico().getId(),
                request.getServico().getNome(),
                request.getAreaM2(),
                request.getStatus(),
                request.getDataCriacao(),
                new AddressSnapshot(
                        request.getEnderecoOrigemId(),
                        request.getEnderecoCep(),
                        request.getEnderecoLogradouro(),
                        request.getEnderecoNumero(),
                        request.getEnderecoComplemento(),
                        request.getEnderecoBairro(),
                        request.getEnderecoCidade(),
                        request.getEnderecoUf(),
                        request.getEnderecoApelido()));
    }

    public record CreateRequest(Integer servicoId, Long enderecoId, BigDecimal areaM2) { }
    public record AddressSnapshot(Long id, String cep, String logradouro, String numero, String complemento,
                                  String bairro, String cidade, String uf, String apelido) { }
    public record ServiceRequestResponse(Long id, Integer servicoId, String servicoNome, BigDecimal areaM2,
                                         ServiceRequestStatus status, LocalDateTime dataCriacao,
                                         AddressSnapshot endereco) { }
    public record ErrorResponse(String message) { }
}
