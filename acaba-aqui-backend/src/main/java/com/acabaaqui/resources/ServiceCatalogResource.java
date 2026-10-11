package com.acabaaqui.resources;

import com.acabaaqui.model.ServiceCategory;
import com.acabaaqui.model.ServiceCatalogItem;
import com.acabaaqui.services.ServiceCatalogService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceCatalogResource {

    @Inject
    ServiceCatalogService service;

    @GET
    @Path("/categories")
    public Response listarCategorias(@QueryParam("ativo") Boolean ativo,
                                     @Context SecurityContext securityContext) {
        if (!isAdministrator(securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(service.listarCategorias(ativo).stream().map(this::toCategoryResponse).toList()).build();
    }

    @POST
    @Path("/categories")
    public Response criarCategoria(CategoryRequest request, @Context SecurityContext securityContext) {
        if (!isAdministrator(securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        if (request == null) {
            return badRequest("Dados da categoria são obrigatórios.");
        }
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(toCategoryResponse(service.criarCategoria(request.nome(), request.descricao())))
                    .build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @PUT
    @Path("/categories/{id}")
    public Response atualizarCategoria(@PathParam("id") Integer id, CategoryRequest request,
                                       @Context SecurityContext securityContext) {
        if (!isAdministrator(securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        if (request == null) {
            return badRequest("Dados da categoria são obrigatórios.");
        }
        try {
            ServiceCategory category = service.atualizarCategoria(
                    id, request.nome(), request.descricao(), request.ativo());
            return category == null ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.ok(toCategoryResponse(category)).build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @DELETE
    @Path("/categories/{id}")
    public Response desativarCategoria(@PathParam("id") Integer id,
                                       @Context SecurityContext securityContext) {
        if (!isAdministrator(securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        ServiceCategory category = service.desativarCategoria(id);
        return category == null ? Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok(toCategoryResponse(category)).build();
    }

    @GET
    @Path("/services")
    public Response listarServicos(@QueryParam("ativo") Boolean ativo,
                                   @Context SecurityContext securityContext) {
        if (!isAdministrator(securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(service.listarServicos(ativo).stream().map(this::toServiceResponse).toList()).build();
    }

    @POST
    @Path("/services")
    public Response criarServico(ServiceRequest request, @Context SecurityContext securityContext) {
        if (!isAdministrator(securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        if (request == null) {
            return badRequest("Dados do serviço são obrigatórios.");
        }
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(toServiceResponse(service.criarServico(
                            request.nome(), request.descricao(), request.categoriaId())))
                    .build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @PUT
    @Path("/services/{id}")
    public Response atualizarServico(@PathParam("id") Integer id, ServiceRequest request,
                                     @Context SecurityContext securityContext) {
        if (!isAdministrator(securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        if (request == null) {
            return badRequest("Dados do serviço são obrigatórios.");
        }
        try {
            ServiceCatalogItem item = service.atualizarServico(
                    id, request.nome(), request.descricao(), request.categoriaId(), request.ativo());
            return item == null ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.ok(toServiceResponse(item)).build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @DELETE
    @Path("/services/{id}")
    public Response desativarServico(@PathParam("id") Integer id,
                                     @Context SecurityContext securityContext) {
        if (!isAdministrator(securityContext)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        ServiceCatalogItem item = service.desativarServico(id);
        return item == null ? Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok(toServiceResponse(item)).build();
    }

    private boolean isAdministrator(SecurityContext securityContext) {
        return securityContext != null && securityContext.isUserInRole("administrador");
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(message)).build();
    }

    private CategoryResponse toCategoryResponse(ServiceCategory category) {
        return new CategoryResponse(category.getId(), category.getNome(), category.getDescricao(), category.isAtivo());
    }

    private ServiceResponse toServiceResponse(ServiceCatalogItem item) {
        return new ServiceResponse(item.getId(), item.getNome(), item.getDescricao(), item.isAtivo(),
                toCategoryResponse(item.getCategoria()));
    }

    public record CategoryRequest(String nome, String descricao, Boolean ativo) { }
    public record ServiceRequest(String nome, String descricao, Integer categoriaId, Boolean ativo) { }
    public record CategoryResponse(Integer id, String nome, String descricao, boolean ativo) { }
    public record ServiceResponse(Integer id, String nome, String descricao, boolean ativo,
                                  CategoryResponse categoria) { }
    public record ErrorResponse(String message) { }
}
