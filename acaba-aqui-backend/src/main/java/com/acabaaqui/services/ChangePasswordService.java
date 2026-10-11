package com.acabaaqui.services;

import com.acabaaqui.model.User;
import com.acabaaqui.model.UserCredential;
import com.acabaaqui.persistence.UserCredentialRepository;
import com.acabaaqui.persistence.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class ChangePasswordService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    @Inject
    UserCredentialRepository credentialRepository;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    UserRepository userRepository;

    @Transactional
    public void alterarSenha(Integer userId, String senhaAtual, String novaSenha) {
        if (userId == null) {
            throw new WebApplicationException("Usuário inválido.", Response.Status.UNAUTHORIZED);
        }
        if (senhaAtual == null || senhaAtual.isBlank()) {
            throw new WebApplicationException("A senha atual é obrigatória.", Response.Status.BAD_REQUEST);
        }
        if (novaSenha == null || novaSenha.isBlank()) {
            throw new WebApplicationException("A nova senha é obrigatória.", Response.Status.BAD_REQUEST);
        }

        UserCredential credential = credentialRepository.buscarPorUsuarioId(userId);
        if (credential == null || Boolean.FALSE.equals(credential.getAtivo())) {
            throw new WebApplicationException("Usuário não encontrado.", Response.Status.NOT_FOUND);
        }
        if (!passwordHasher.verificarSenha(senhaAtual, credential)) {
            throw new WebApplicationException("A senha atual está incorreta.", Response.Status.UNAUTHORIZED);
        }
        if (!isNovaSenhaValida(novaSenha, senhaAtual)) {
            throw new WebApplicationException("A nova senha deve ter pelo menos 8 caracteres e não pode ser igual à atual.", Response.Status.BAD_REQUEST);
        }
        if (passwordHasher.verificarSenha(novaSenha, credential)) {
            throw new WebApplicationException("A nova senha deve ser diferente da atual.", Response.Status.BAD_REQUEST);
        }

        passwordHasher.definirSenha(credential, novaSenha);
        credential.setVersaoToken((credential.getVersaoToken() == null ? 0 : credential.getVersaoToken()) + 1);

        User user = userRepository.buscarUsuarioPorId(userId);
        if (user == null || Boolean.FALSE.equals(user.getAtivo())) {
            throw new WebApplicationException("Usuário não encontrado.", Response.Status.NOT_FOUND);
        }

        credentialRepository.getEntityManager().flush();
    }

    private boolean isNovaSenhaValida(String novaSenha, String senhaAtual) {
        return novaSenha.length() >= MIN_PASSWORD_LENGTH && !novaSenha.equals(senhaAtual);
    }
}
