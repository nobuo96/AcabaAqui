package com.acabaaqui.persistence;

import com.acabaaqui.model.RequestOpportunity;
import com.acabaaqui.model.RequestOpportunityStatus;
import com.acabaaqui.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.List;

@ApplicationScoped
public class RequestOpportunityRepository implements PanacheRepository<RequestOpportunity> {

    public List<RequestOpportunity> listarPorPrestador(User prestador) {
        return list("select o from RequestOpportunity o join fetch o.solicitacao r "
                + "join fetch r.servico where o.prestador = ?1 "
                + "order by o.dataCriacao desc, o.id desc", prestador);
    }

    public RequestOpportunity buscarParaPrestador(Long id, User prestador) {
        return find("id = ?1 and prestador = ?2", id, prestador).firstResult();
    }

    public RequestOpportunity buscarComBloqueio(Long id, User prestador) {
        return find("id = ?1 and prestador = ?2", id, prestador)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }

    public List<RequestOpportunity> listarPorSolicitacaoComBloqueio(Long requestId) {
        return find("solicitacao.id = ?1", requestId)
                .withLock(LockModeType.PESSIMISTIC_READ).list();
    }

    public boolean temOportunidadeAceita(Long requestId, Integer providerId) {
        return count("solicitacao.id = ?1 and prestador.id = ?2 and status = ?3",
                requestId, providerId, RequestOpportunityStatus.ACEITA) > 0;
    }

}
