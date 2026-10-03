package com.acabaaqui.persistence;

import com.acabaaqui.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User>{

    @Transactional
    public void adicionarUsuario(User usuario) {
        persist(usuario);
    }

    public User buscarUsuarioPorId(Integer id) {
        return findById((long) id);
    }

    public User buscarUsuarioPorEmail(String email) {
        return find("email", email).firstResult();
    }
}
