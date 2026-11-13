package com.acabaaqui.persistence;

import com.acabaaqui.model.Conversation;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ConversationRepository implements PanacheRepository<Conversation> {

    public Conversation buscarPorSolicitacaoEPrestador(Long requestId, Integer providerId) {
        return find("solicitacao.id = ?1 and prestador.id = ?2", requestId, providerId).firstResult();
    }

    public List<Conversation> listarDoUsuario(Integer userId) {
        return list("select c from Conversation c join fetch c.solicitacao r "
                        + "join fetch r.servico join fetch c.cliente join fetch c.prestador "
                        + "where c.cliente.id = ?1 or c.prestador.id = ?1 "
                        + "order by c.dataCriacao desc, c.id desc", userId);
    }

    public Conversation buscarParaUsuario(Long id, Integer userId) {
        return find("id = ?1 and (cliente.id = ?2 or prestador.id = ?2)", id, userId).firstResult();
    }
}
