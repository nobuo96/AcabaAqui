package com.acabaaqui.resources;

import com.acabaaqui.persistence.UserRepository;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import com.acabaaqui.model.User;
import jakarta.inject.Inject;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserRepository repository;

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

}