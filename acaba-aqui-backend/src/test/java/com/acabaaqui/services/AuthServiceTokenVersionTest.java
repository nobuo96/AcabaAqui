package com.acabaaqui.services;

import com.acabaaqui.model.PerfilUser;
import com.acabaaqui.model.User;
import com.acabaaqui.model.UserCredential;
import com.acabaaqui.persistence.UserCredentialRepository;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTokenVersionTest {

    @Test
    void rejectsTokenAfterCredentialVersionChanges() {
        UserCredential credential = new UserCredential();
        credential.setUsuarioId(42);
        credential.setVersaoToken(0);
        credential.setAtivo(true);

        AuthService authService = new AuthService();
        authService.jwtSecret = Base64.getEncoder().encodeToString(new byte[32]);
        authService.jwtIssuer = "test-issuer";
        authService.tokenDurationSeconds = 3600;
        authService.credentialRepository = new UserCredentialRepository() {
            @Override
            public UserCredential buscarPorUsuarioId(Integer userId) {
                return credential.getUsuarioId().equals(userId) ? credential : null;
            }
        };

        User user = new User();
        user.setId(42);
        user.setPerfil(PerfilUser.cliente);
        String token = authService.gerarToken(user);
        assertEquals("42", authService.validarToken(token).getSubject());

        credential.setVersaoToken(1);
        assertThrows(WebApplicationException.class, () -> authService.validarToken(token));
    }
}