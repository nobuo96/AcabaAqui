package com.acabaaqui.persistence;

import com.acabaaqui.model.ChatMessage;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ChatMessageRepository implements PanacheRepository<ChatMessage> {

    public List<ChatMessage> listarPorConversa(Long conversationId) {
        return list("select m from ChatMessage m join fetch m.remetente "
                + "where m.conversa.id = ?1 order by m.dataCriacao, m.id", conversationId);
    }
}
