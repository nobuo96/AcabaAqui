package com.acabaaqui.services;

import com.acabaaqui.model.LifecycleStatusHistory;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.LifecycleStatusHistoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class LifecycleStatusService {

    @Inject
    LifecycleStatusHistoryRepository historyRepository;

    @Transactional
    public void registrarTransicao(
            String entityType,
            Long entityId,
            String previousStatus,
            String newStatus,
            String actorType,
            User actor) {
        LifecycleStatusTransitionPolicy.validar(entityType, previousStatus, newStatus, actorType);

        LifecycleStatusHistory history = new LifecycleStatusHistory();
        history.setEntityType(entityType);
        history.setEntityId(entityId);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setActorType(actorType);
        history.setActor(actor);
        historyRepository.persist(history);
    }
}
