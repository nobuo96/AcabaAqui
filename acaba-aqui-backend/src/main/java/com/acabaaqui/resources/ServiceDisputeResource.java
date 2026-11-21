package com.acabaaqui.resources;

import com.acabaaqui.model.ServiceDisputeDecision;
import com.acabaaqui.services.ServiceDisputeService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.function.Supplier;

@Path("/disputes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceDisputeResource {

    @Inject
    ServiceDisputeService service;

    @GET
    public Response listar(@Context SecurityContext context) {
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(service.listarParaAdministrador()).build();
    }

    @GET
    @Path("/quotes/{quoteId}")
    public Response buscarPorOrcamento(
            @PathParam("quoteId") Long quoteId,
            @Context SecurityContext context) {
        Integer userId = userId(context);
        if (userId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            ServiceDisputeService.DisputeResponse dispute =
                    service.buscarPorOrcamento(quoteId, userId, isAdmin(context));
            return dispute == null ? Response.noContent().build() : Response.ok(dispute).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        }
    }

    @POST
    @Path("/quotes/{quoteId}")
    public Response abrir(
            @PathParam("quoteId") Long quoteId,
            OpenDisputeRequest request,
            @Context SecurityContext context) {
        Integer clientId = userId(context, "cliente");
        if (clientId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return invoke(() -> service.abrir(clientId, quoteId, request == null ? null : request.reason()));
    }

    @GET
    @Path("/{disputeId}")
    public Response buscar(
            @PathParam("disputeId") Long disputeId,
            @Context SecurityContext context) {
        Integer userId = userId(context);
        if (userId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return invoke(() -> service.buscar(disputeId, userId, isAdmin(context)));
    }

    @GET
    @Path("/{disputeId}/evidence")
    public Response listarEvidencias(
            @PathParam("disputeId") Long disputeId,
            @Context SecurityContext context) {
        Integer userId = userId(context);
        if (userId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return invoke(() -> service.listarEvidencias(disputeId, userId, isAdmin(context)));
    }

    @POST
    @Path("/{disputeId}/evidence")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response anexarEvidencia(
            @PathParam("disputeId") Long disputeId,
            @RestForm("file") FileUpload file,
            @Context SecurityContext context) {
        Integer userId = userId(context);
        if (userId == null || (!context.isUserInRole("cliente") && !context.isUserInRole("prestador"))) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        if (file == null || file.size() > 10L * 1024 * 1024) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Cada evidência pode ter no máximo 10 MB."))
                    .build();
        }
        try {
            byte[] content = java.nio.file.Files.readAllBytes(file.uploadedFile());
            return Response.ok(service.anexarEvidencia(
                    disputeId, userId, false, file.fileName(), file.contentType(), content)).build();
        } catch (java.io.IOException exception) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Não foi possível ler o arquivo enviado."))
                    .build();
        } catch (WebApplicationException exception) {
            return error(exception);
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        } catch (IllegalStateException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{disputeId}/evidence/{evidenceId}")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public Response baixarEvidencia(
            @PathParam("disputeId") Long disputeId,
            @PathParam("evidenceId") Long evidenceId,
            @Context SecurityContext context) {
        Integer userId = userId(context);
        if (userId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            ServiceDisputeService.EvidenceDownload file =
                    service.baixarEvidencia(disputeId, evidenceId, userId, isAdmin(context));
            String safeFilename = file.filename().replace("\"", "").replace("\r", "").replace("\n", "");
            return Response.ok(file.content(), file.contentType())
                    .header("Content-Disposition", "attachment; filename=\"" + safeFilename + "\"")
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        } catch (WebApplicationException exception) {
            return error(exception);
        }
    }

    @POST
    @Path("/{disputeId}/decision")
    public Response decidir(
            @PathParam("disputeId") Long disputeId,
            DecisionRequest request,
            @Context SecurityContext context) {
        Integer adminId = userId(context, "administrador");
        if (adminId == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return invoke(() -> service.decidir(
                disputeId,
                adminId,
                request == null ? null : request.decision(),
                request == null ? null : request.reason()));
    }

    private Response invoke(Supplier<Object> operation) {
        try {
            return Response.ok(operation.get()).build();
        } catch (WebApplicationException exception) {
            return error(exception);
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        } catch (IllegalStateException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(exception.getMessage()))
                    .build();
        }
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

    private Integer userId(SecurityContext context, String role) {
        return context != null && context.isUserInRole(role) ? userId(context) : null;
    }

    private boolean isAdmin(SecurityContext context) {
        return context != null && context.isUserInRole("administrador");
    }

    private Response error(WebApplicationException exception) {
        return Response.status(exception.getResponse().getStatus())
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
    }

    public record OpenDisputeRequest(String reason) { }
    public record DecisionRequest(ServiceDisputeDecision decision, String reason) { }
    public record ErrorResponse(String message) { }
}
