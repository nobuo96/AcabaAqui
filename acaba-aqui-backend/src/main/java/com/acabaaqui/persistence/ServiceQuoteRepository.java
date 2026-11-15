package com.acabaaqui.persistence;

import com.acabaaqui.model.ServiceQuote;
import com.acabaaqui.model.ServiceQuoteStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ServiceQuoteRepository implements PanacheRepository<ServiceQuote> {

    public Long buscarSolicitacaoId(Long quoteId) {
        return getEntityManager().createQuery(
                        "select q.solicitacao.id from ServiceQuote q where q.id = :quoteId", Long.class)
                .setParameter("quoteId", quoteId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public List<ServiceQuote> listarPorSolicitacaoComBloqueio(Long requestId) {
        return find("select q from ServiceQuote q join fetch q.prestador "
                        + "join fetch q.solicitacao where q.solicitacao.id = ?1",
                requestId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).list();
    }

    public ServiceQuote buscarPorSolicitacaoEPrestador(Long requestId, Integer providerId) {
        return find("solicitacao.id = ?1 and prestador.id = ?2", requestId, providerId).firstResult();
    }

    public List<ExpirationCandidate> listarExpiradosAte(LocalDateTime cutoff) {
        return getEntityManager().createQuery(
                        "select q.id, q.solicitacao.id from ServiceQuote q "
                                + "where q.status = :status and q.expiraEm <= :cutoff order by q.id",
                        Object[].class)
                .setParameter("status", ServiceQuoteStatus.ENVIADO)
                .setParameter("cutoff", cutoff)
                .getResultList().stream()
                .map(row -> new ExpirationCandidate((Long) row[0], (Long) row[1]))
                .toList();
    }

    public record ExpirationCandidate(Long quoteId, Long requestId) { }
}
