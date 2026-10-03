package com.acabaaqui.resources;

import com.acabaaqui.model.User;
import com.acabaaqui.services.AuthService;
import com.acabaaqui.services.EmailVerificationService;
import com.acabaaqui.services.PasswordResetService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    EmailVerificationService emailVerificationService;

    @Inject
    PasswordResetService passwordResetService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        if (request == null || request.email() == null || request.senha() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("E-mail e senha são obrigatórios").build();
        }

        String token = authService.autenticar(request.email().trim().toLowerCase(), request.senha());
        return Response.ok(new LoginResponse(token, authService.getUsuarioId(token), authService.getPerfil(token),
            authService.getDuracaoTokenSegundos())).build();
    }

    @POST
    @Path("/email-verification/request")
    public Response solicitarCodigo(RegistrationRequest request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Dados do cadastro obrigatórios").build();
        }

        User user = new User();
        user.setNome(request.nome());
        user.setEmail(request.email());
        user.setTelefone(request.telefone());
        user.setPerfil(request.perfil());

        try {
            emailVerificationService.solicitarCodigo(user, request.senha());
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
        }

        return Response.accepted(new MessageResponse(
                "Se o cadastro puder prosseguir, enviaremos um código para o e-mail informado.")).build();
    }

    @POST
    @Path("/email-verification/resend")
    public Response reenviarCodigo(EmailRequest request) {
        if (request == null || request.email() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("E-mail obrigatório").build();
        }

        try {
            emailVerificationService.reenviarCodigo(request.email());
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
        }

        return Response.accepted(new MessageResponse(
                "Se o cadastro puder prosseguir, enviaremos um novo código para o e-mail informado.")).build();
    }

    @POST
    @Path("/email-verification/confirm")
    public Response confirmarEmail(VerificationRequest request) {
        if (request == null || request.email() == null || request.code() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("E-mail e código são obrigatórios").build();
        }

        EmailVerificationService.ConfirmationResult result;
        try {
            result = emailVerificationService.confirmar(request.email(), request.code());
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
        }

        if (result.status() == EmailVerificationService.ConfirmationStatus.TOO_MANY_ATTEMPTS) {
            return Response.status(429).entity(new MessageResponse(
                    "Limite de tentativas atingido. Solicite um novo código.")).build();
        }
        if (result.status() != EmailVerificationService.ConfirmationStatus.VERIFIED) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new MessageResponse(
                    "Código inválido ou expirado.")).build();
        }

        return Response.ok(new LoginResponse(result.token(), result.userId(), result.perfil(),
                authService.getDuracaoTokenSegundos())).build();
    }

    @POST
    @Path("/password-reset/request")
    public Response solicitarRedefinicao(EmailRequest request) {
        if (request == null || request.email() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new MessageResponse("Informe um e-mail válido.")).build();
        }

        try {
            passwordResetService.solicitar(request.email());
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new MessageResponse(exception.getMessage())).build();
        }

        return Response.accepted(new MessageResponse(
                "Se houver uma conta elegível, enviaremos instruções para o e-mail informado.")).build();
    }

    @POST
    @Path("/password-reset/resend")
    public Response reenviarRedefinicao(EmailRequest request) {
        if (request == null || request.email() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new MessageResponse("Informe um e-mail válido.")).build();
        }

        try {
            passwordResetService.reenviar(request.email());
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new MessageResponse(exception.getMessage())).build();
        }

        return Response.accepted(new MessageResponse(
                "Se houver uma solicitação ativa, enviaremos um novo código para o e-mail informado.")).build();
    }

    @POST
    @Path("/password-reset/confirm")
    public Response confirmarRedefinicao(PasswordResetRequest request) {
        if (request == null || request.email() == null || request.code() == null || request.newPassword() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new MessageResponse("E-mail, código e nova senha são obrigatórios.")).build();
        }

        try {
            if (!passwordResetService.confirmar(request.email(), request.code(), request.newPassword())) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new MessageResponse("Código inválido ou expirado.")).build();
            }
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new MessageResponse(exception.getMessage())).build();
        }

        return Response.ok(new MessageResponse("Senha redefinida. Entre novamente com a nova senha.")).build();
    }

    public record LoginRequest(String email, String senha) { }

    public record RegistrationRequest(String nome, String email, String telefone,
                                      com.acabaaqui.model.PerfilUser perfil, String senha) { }

    public record EmailRequest(String email) { }

    public record VerificationRequest(String email, String code) { }

    public record PasswordResetRequest(String email, String code, String newPassword) { }

    public record MessageResponse(String message) { }

    public record LoginResponse(String token, Integer userId, String perfil, long expiresInSeconds) { }
}