package com.acabaaqui.persistence;

import com.acabaaqui.model.LifecycleStatusHistory;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class LifecycleStatusHistoryRepository implements PanacheRepository<LifecycleStatusHistory> {

    public boolean existeStatus(String entityType, Long entityId, String status) {
        return count("entityType = ?1 and entityId = ?2 and newStatus = ?3",
                entityType, entityId, status) > 0;
    }

    public boolean existeAlgumStatus(String entityType, Long entityId, List<String> statuses) {
        return count("entityType = ?1 and entityId = ?2 and newStatus in ?3",
                entityType, entityId, statuses) > 0;
    }

    public String buscarUltimoStatus(String entityType, Long entityId) {
        List<String> statuses = getEntityManager().createQuery(
                        "select h.newStatus from LifecycleStatusHistory h "
                                + "where h.entityType = :entityType and h.entityId = :entityId "
                                + "order by h.occurredAt desc, h.id desc",
                        String.class)
                .setParameter("entityType", entityType)
                .setParameter("entityId", entityId)
                .setMaxResults(1)
                .getResultList();
        return statuses.isEmpty() ? null : statuses.get(0);
    }

    public List<LifecycleStatusHistory> listarPorEntidade(String entityType, Long entityId) {
        return list("entityType = ?1 and entityId = ?2 order by occurredAt, id", entityType, entityId);
    }
}
