package com.acabaaqui.resources;

import com.acabaaqui.model.Address;
import com.acabaaqui.services.AddressService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

@Path("/user/{userId}/addresses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AddressResource {

    @Inject
    AddressService addressService;

    @GET
    public Response listar(@PathParam("userId") Integer userId, @Context SecurityContext securityContext) {
        if (!isAuthorized(userId, securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        var addresses = addressService.listar(userId);
        return addresses == null ? notFound("Usuário não encontrado.")
                : Response.ok(addresses.stream().map(this::toResponse).toList()).build();
    }

    @POST
    public Response criar(@PathParam("userId") Integer userId, AddressService.AddressRequest request,
                         @Context SecurityContext securityContext) {
        if (!isAuthorized(userId, securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            Address address = addressService.criar(userId, request);
            return address == null ? notFound("Usuário não encontrado.")
                    : Response.status(Response.Status.CREATED).entity(toResponse(address)).build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @PUT
    @Path("/{addressId}")
    public Response atualizar(@PathParam("userId") Integer userId, @PathParam("addressId") Long addressId,
                              AddressService.AddressRequest request,
                              @Context SecurityContext securityContext) {
        if (!isAuthorized(userId, securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            Address address = addressService.atualizar(userId, addressId, request);
            return address == null ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.ok(toResponse(address)).build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @PUT
    @Path("/{addressId}/principal")
    public Response definirPrincipal(@PathParam("userId") Integer userId, @PathParam("addressId") Long addressId,
                                     @Context SecurityContext securityContext) {
        if (!isAuthorized(userId, securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        Address address = addressService.definirPrincipal(userId, addressId);
        return address == null ? Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok(toResponse(address)).build();
    }

    @DELETE
    @Path("/{addressId}")
    public Response desativar(@PathParam("userId") Integer userId, @PathParam("addressId") Long addressId,
                              @Context SecurityContext securityContext) {
        if (!isAuthorized(userId, securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        Address address = addressService.desativar(userId, addressId);
        return address == null ? Response.status(Response.Status.NOT_FOUND).build()
                : Response.noContent().build();
    }

    private boolean isAuthorized(Integer userId, SecurityContext securityContext) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return false;
        }
        return securityContext.getUserPrincipal().getName().equals(userId.toString())
                || securityContext.isUserInRole("administrador");
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(address.getId(), address.getCep(), address.getLogradouro(), address.getNumero(),
                address.getComplemento(), address.getBairro(), address.getCidade(), address.getUf(),
                address.getApelido(), address.isPrincipal());
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(message)).build();
    }

    private Response notFound(String message) {
        return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(message)).build();
    }

    public record AddressResponse(Long id, String cep, String logradouro, String numero, String complemento,
                                  String bairro, String cidade, String uf, String apelido, boolean principal) { }
    public record ErrorResponse(String message) { }
}
