package com.acabaaqui.services;

import com.acabaaqui.model.PerfilUser;
import com.acabaaqui.model.User;
import com.acabaaqui.model.UserCredential;
import com.acabaaqui.persistence.UserCredentialRepository;
import com.acabaaqui.persistence.UserRepository;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangePasswordServiceTest {

    @Test
    void rejectsWrongCurrentPassword() {
        TestCredentialRepository repository = new TestCredentialRepository();
        UserCredential credential = new UserCredential();
        credential.setUsuarioId(42);
        credential.setAtivo(true);
        credential.setVersaoToken(1);
        new PasswordHasher().definirSenha(credential, "SenhaCorreta123");
        repository.credential = credential;

        ChangePasswordService service = new ChangePasswordService();
        service.passwordHasher = new PasswordHasher();
        service.credentialRepository = repository;

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> service.alterarSenha(42, "SenhaErrada123", "NovaSenha456"));
        assertEquals(401, exception.getResponse().getStatus());
        assertEquals(1, credential.getVersaoToken());
    }

    @Test
    void rejectsWeakNewPassword() {
        TestCredentialRepository repository = new TestCredentialRepository();
        UserCredential credential = new UserCredential();
        credential.setUsuarioId(42);
        credential.setAtivo(true);
        credential.setVersaoToken(1);
        new PasswordHasher().definirSenha(credential, "SenhaCorreta123");
        repository.credential = credential;

        ChangePasswordService service = new ChangePasswordService();
        service.passwordHasher = new PasswordHasher();
        service.credentialRepository = repository;

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> service.alterarSenha(42, "SenhaCorreta123", "fraca"));
        assertEquals(400, exception.getResponse().getStatus());
        assertTrue(new PasswordHasher().verificarSenha("SenhaCorreta123", credential));
    }

    @Test
    void rejectsPasswordReuse() {
        TestCredentialRepository repository = new TestCredentialRepository();
        UserCredential credential = new UserCredential();
        credential.setUsuarioId(42);
        credential.setAtivo(true);
        credential.setVersaoToken(1);
        new PasswordHasher().definirSenha(credential, "SenhaCorreta123");
        repository.credential = credential;

        ChangePasswordService service = new ChangePasswordService();
        service.passwordHasher = new PasswordHasher();
        service.credentialRepository = repository;

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> service.alterarSenha(42, "SenhaCorreta123", "SenhaCorreta123"));
        assertEquals(400, exception.getResponse().getStatus());
        assertEquals(1, credential.getVersaoToken());
    }

    private static class TestCredentialRepository extends UserCredentialRepository {
        private UserCredential credential;

        @Override
        public UserCredential buscarPorUsuarioId(Integer usuarioId) {
            return usuarioId.equals(credential.getUsuarioId()) ? credential : null;
        }
    }
}
