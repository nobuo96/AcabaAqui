package com.acabaaqui.resources;

import com.acabaaqui.persistence.UserRepository;
import com.acabaaqui.services.UserService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import com.acabaaqui.model.User;
import jakarta.inject.Inject;
import java.time.LocalDate;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserRepository repository;

    @Inject
    UserService service;

    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") Integer id, @Context SecurityContext securityContext) {
        String authenticatedUserId = securityContext.getUserPrincipal().getName();
        if (!authenticatedUserId.equals(id.toString())
                && !securityContext.isUserInRole("administrador")) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        User user = repository.buscarUsuarioPorId(id);
        return user == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(user).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateUser(@PathParam("id") Integer id, UpdatePersonalInfoRequest request,
                               @Context SecurityContext securityContext) {
        String authenticatedUserId = securityContext.getUserPrincipal().getName();
        if (!authenticatedUserId.equals(id.toString())
                && !securityContext.isUserInRole("administrador")) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Dados pessoais obrigatórios.").build();
        }

        try {
            User updatedUser = service.atualizarDadosPessoais(
                    id, request.nome(), request.telefone(), request.dataNascimento());
            return updatedUser == null
                    ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.ok(updatedUser).build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
        }
    }

    public record UpdatePersonalInfoRequest(String nome, String telefone, LocalDate dataNascimento) { }

}