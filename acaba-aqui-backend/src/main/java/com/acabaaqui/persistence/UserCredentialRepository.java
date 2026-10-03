package com.acabaaqui.persistence;

import com.acabaaqui.model.UserCredential;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserCredentialRepository implements PanacheRepository<UserCredential> {

    public UserCredential buscarPorUsuarioId(Integer usuarioId) {
        return find("usuarioId", usuarioId).firstResult();
    }

    @Transactional
    public void adicionar(UserCredential credencial) {
        persist(credencial);
    }
}