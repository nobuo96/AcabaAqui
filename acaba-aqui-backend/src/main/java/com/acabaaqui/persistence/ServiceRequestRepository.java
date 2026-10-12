package com.acabaaqui.persistence;

import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.List;

@ApplicationScoped
public class ServiceRequestRepository implements PanacheRepository<ServiceRequest> {

    public List<ServiceRequest> listarPorUsuario(User usuario) {
        return list("select r from ServiceRequest r join fetch r.servico "
                + "where r.usuario = ?1 order by r.dataCriacao desc, r.id desc", usuario);
    }

    public ServiceRequest buscarComBloqueio(Long id) {
        return findById(id, LockModeType.PESSIMISTIC_WRITE);
    }
}
