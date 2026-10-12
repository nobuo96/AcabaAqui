package com.acabaaqui.persistence;

import com.acabaaqui.model.Address;
import com.acabaaqui.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AddressRepository implements PanacheRepository<Address> {

    public List<Address> listarAtivos(User usuario) {
        return list("usuario = ?1 and ativo = true order by principal desc, apelido asc, id asc", usuario);
    }
}
