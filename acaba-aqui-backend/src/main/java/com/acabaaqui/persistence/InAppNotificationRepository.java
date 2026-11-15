package com.acabaaqui.persistence;

import com.acabaaqui.model.InAppNotification;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.List;

@ApplicationScoped
public class InAppNotificationRepository implements PanacheRepository<InAppNotification> {

    public List<InAppNotification> listarPorUsuario(Integer userId) {
        return list("select n from InAppNotification n "
                        + "left join fetch n.solicitacao left join fetch n.orcamento "
                        + "where n.usuario.id = ?1 order by n.dataCriacao desc, n.id desc",
                userId);
    }

    public InAppNotification buscarParaUsuarioComBloqueio(Long notificationId, Integer userId) {
        return find("id = ?1 and usuario.id = ?2", notificationId, userId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }
}
