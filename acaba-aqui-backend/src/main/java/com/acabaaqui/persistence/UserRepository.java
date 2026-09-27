package com.acabaaqui.persistence;

import com.acabaaqui.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User>{
    
    public void adicionarUsuario(User usuario) {
        persist(usuario);
    }

    public User buscarUsuarioPorId(Long id) {
        return findById(id);
    }
}
