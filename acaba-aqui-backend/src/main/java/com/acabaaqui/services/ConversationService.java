package com.acabaaqui.services;

import com.acabaaqui.model.ChatMessage;
import com.acabaaqui.model.Conversation;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.ChatMessageRepository;
import com.acabaaqui.persistence.ConversationRepository;
import com.acabaaqui.persistence.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class ConversationService {

    @Inject
    ConversationRepository conversationRepository;

    @Inject
    ChatMessageRepository messageRepository;

    @Inject
    UserRepository userRepository;

    public List<Conversation> listar(Integer userId) {
        return conversationRepository.listarDoUsuario(userId);
    }

    public List<ChatMessage> listarMensagens(Integer userId, Long conversationId) {
        Conversation conversation = conversationRepository.buscarParaUsuario(conversationId, userId);
        return conversation == null ? null : messageRepository.listarPorConversa(conversationId);
    }

    @Transactional
    public ChatMessage enviar(Integer userId, Long conversationId, String content) {
        if (content == null || content.isBlank() || content.trim().length() > 4000) {
            throw new IllegalArgumentException("A mensagem deve ter entre 1 e 4000 caracteres.");
        }
        Conversation conversation = conversationRepository.buscarParaUsuario(conversationId, userId);
        User sender = userRepository.buscarUsuarioPorId(userId);
        if (conversation == null || sender == null) {
            return null;
        }
        ChatMessage message = new ChatMessage();
        message.setConversa(conversation);
        message.setRemetente(sender);
        message.setConteudo(content.trim());
        messageRepository.persist(message);
        return message;
    }
}
