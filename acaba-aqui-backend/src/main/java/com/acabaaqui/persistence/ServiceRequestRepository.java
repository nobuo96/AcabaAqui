package com.acabaaqui.persistence;

import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ServiceRequestRepository implements PanacheRepository<ServiceRequest> {

    public ServiceRequest buscarPorId(Long id) {
        return findById(id);
    }

    public List<ServiceRequest> listarPorUsuario(User usuario) {
        return list("select r from ServiceRequest r join fetch r.servico "
                + "where r.usuario = ?1 order by r.dataCriacao desc, r.id desc", usuario);
    }

    public ServiceRequest buscarComBloqueio(Long id) {
        return findById(id, LockModeType.PESSIMISTIC_WRITE);
    }

    public List<Long> listarIdsCriadasAte(LocalDateTime cutoff) {
        return getEntityManager().createQuery(
                        "select r.id from ServiceRequest r "
                                + "where r.status = :status and r.dataCriacao <= :cutoff order by r.id",
                        Long.class)
                .setParameter("status", ServiceRequestStatus.CRIADA)
                .setParameter("cutoff", cutoff)
                .getResultList();
    }
}
