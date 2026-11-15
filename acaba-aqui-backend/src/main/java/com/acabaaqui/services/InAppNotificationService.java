package com.acabaaqui.services;

import com.acabaaqui.model.InAppNotification;
import com.acabaaqui.model.ServiceQuote;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.InAppNotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class InAppNotificationService {

    @Inject
    InAppNotificationRepository notificationRepository;

    @Transactional
    public void criar(User recipient, ServiceRequest request, ServiceQuote quote, String type, String message) {
        InAppNotification notification = new InAppNotification();
        notification.setUsuario(recipient);
        notification.setSolicitacao(request);
        notification.setOrcamento(quote);
        notification.setTipo(type);
        notification.setMensagem(message);
        notificationRepository.persist(notification);
    }

    public List<InAppNotification> listar(Integer userId) {
        return notificationRepository.listarPorUsuario(userId);
    }

    @Transactional
    public boolean marcarLida(Long notificationId, Integer userId) {
        InAppNotification notification =
                notificationRepository.buscarParaUsuarioComBloqueio(notificationId, userId);
        if (notification == null) {
            return false;
        }
        notification.setLida(true);
        return true;
    }
}
