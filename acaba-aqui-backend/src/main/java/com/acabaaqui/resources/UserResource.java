package com.acabaaqui.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.acabaaqui.model.User;
import com.acabaaqui.persistence.UserRepository;
import jakarta.inject.Inject;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserRepository repository;

    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") String id) {
        

        return Response.ok().build();
    }

    @POST
    public Response addUser(User user) {
        // Aqui você pode adicionar a lógica para salvar o usuário no banco de dados
        repository.adicionarUsuario(user);
        return Response.status(Response.Status.CREATED).entity(user).build();
    }
}