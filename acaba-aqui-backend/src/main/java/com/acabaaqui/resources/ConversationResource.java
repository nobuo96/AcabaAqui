package com.acabaaqui.resources;

import com.acabaaqui.model.ChatMessage;
import com.acabaaqui.model.Conversation;
import com.acabaaqui.services.ConversationService;
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

import java.time.LocalDateTime;

@Path("/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConversationResource {

    @Inject
    ConversationService service;

    @GET
    public Response listar(@Context SecurityContext context) {
        Integer userId = userId(context);
        if (userId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(service.listar(userId).stream().map(this::toResponse).toList()).build();
    }

    @GET
    @Path("/{conversationId}/messages")
    public Response listarMensagens(@PathParam("conversationId") Long conversationId,
                                    @Context SecurityContext context) {
        Integer userId = userId(context);
        if (userId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        var messages = service.listarMensagens(userId, conversationId);
        return messages == null ? Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok(messages.stream().map(this::toResponse).toList()).build();
    }

    @POST
    @Path("/{conversationId}/messages")
    public Response enviar(@PathParam("conversationId") Long conversationId, SendMessage request,
                           @Context SecurityContext context) {
        Integer userId = userId(context);
        if (userId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        try {
            ChatMessage message = service.enviar(userId, conversationId,
                    request == null ? null : request.conteudo());
            return message == null ? Response.status(Response.Status.NOT_FOUND).build()
                    : Response.status(Response.Status.CREATED).entity(toResponse(message)).build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(exception.getMessage())).build();
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

    private ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(conversation.getId(), conversation.getSolicitacao().getServico().getNome(),
                conversation.getCliente().getId(), conversation.getCliente().getNome(),
                conversation.getPrestador().getId(), conversation.getPrestador().getNome(),
                conversation.getSolicitacao().getEnderecoCidade(), conversation.getSolicitacao().getEnderecoUf(),
                conversation.getDataCriacao());
    }

    private MessageResponse toResponse(ChatMessage message) {
        return new MessageResponse(message.getId(), message.getConversa().getId(), message.getRemetente().getId(),
                message.getRemetente().getNome(), message.getConteudo(), message.getDataCriacao());
    }

    public record SendMessage(String conteudo) { }
    public record ConversationResponse(Long id, String servicoNome, Integer clienteId, String clienteNome,
                                       Integer prestadorId, String prestadorNome, String cidade, String uf,
                                       LocalDateTime dataCriacao) { }
    public record MessageResponse(Long id, Long conversaId, Integer remetenteId, String remetenteNome,
                                  String conteudo, LocalDateTime dataCriacao) { }
    public record ErrorResponse(String message) { }
}
