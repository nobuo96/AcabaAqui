package com.acabaaqui.services;

import com.acabaaqui.model.User;
import com.acabaaqui.model.UserCredential;
import com.acabaaqui.model.AlgoritmoCredential;
import com.acabaaqui.persistence.UserRepository;
import com.acabaaqui.persistence.UserCredentialRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository repository;

    @Inject
    UserCredentialRepository credentialRepository;

    @Transactional
    public User cadastrarUsuarioVerificado(User user, UserCredential credential) {
        if (repository.buscarUsuarioPorEmail(user.getEmail()) != null) {
            throw new IllegalStateException("E-mail já cadastrado");
        }

        user.setAtivo(true);
        repository.adicionarUsuario(user);
        credential.setUsuarioId(user.getId());
        credential.setAtivo(true);
        credentialRepository.adicionar(credential);

        return user;
    }

    public void validarCadastro(User user, String senha) {
        if (user == null || user.getNome() == null || user.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome obrigatório");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("E-mail obrigatório");
        }

        if (senha == null || senha.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres");
        }

        if (user.getPerfil() == null || !user.perfilValido(user)) {
            throw new IllegalArgumentException("Perfil inválido");
        }

        if (user.getTelefone() == null || user.getTelefone().isBlank()) {
            throw new IllegalArgumentException("Telefone obrigatório");
        }
    }
}
