package com.acabaaqui.persistence;

import com.acabaaqui.model.ServiceDispute;
import com.acabaaqui.model.ServiceDisputeEvidence;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ServiceDisputeRepository implements PanacheRepository<ServiceDispute> {

    public ServiceDispute buscarPorOrcamento(Long quoteId) {
        return find("select d from ServiceDispute d join fetch d.orcamento q "
                        + "join fetch q.prestador join fetch q.solicitacao r "
                        + "join fetch r.usuario join fetch r.servico where q.id = ?1",
                quoteId).firstResult();
    }

    public ServiceDispute buscarPorIdComBloqueio(Long disputeId) {
        return find("id = ?1", disputeId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }

    public ServiceDispute buscarDetalhe(Long disputeId) {
        return find("select d from ServiceDispute d join fetch d.orcamento q "
                        + "join fetch q.prestador join fetch q.solicitacao r "
                        + "join fetch r.usuario join fetch r.servico where d.id = ?1",
                disputeId).firstResult();
    }

    public List<ServiceDispute> listarTodos() {
        return find("select d from ServiceDispute d join fetch d.orcamento q "
                        + "join fetch q.prestador join fetch q.solicitacao r "
                        + "join fetch r.usuario join fetch r.servico "
                        + "order by case when d.status = 'ABERTA' then 0 else 1 end, d.abertaEm desc, d.id desc")
                .list();
    }

    public int contarEvidenciasPorParticipante(Long disputeId, Integer userId) {
        return (int) getEntityManager().createQuery(
                        "select count(e) from ServiceDisputeEvidence e "
                                + "where e.disputa.id = :disputeId and e.enviadoPor.id = :userId",
                        Long.class)
                .setParameter("disputeId", disputeId)
                .setParameter("userId", userId)
                .getSingleResult().longValue();
    }

    public List<Object[]> listarMetadadosEvidencias(Long disputeId) {
        return getEntityManager().createQuery(
                        "select e.id, e.enviadoPor.id, e.enviadoPor.nome, e.nomeArquivo, "
                                + "e.tipoConteudo, e.tamanhoBytes, e.criadaEm "
                                + "from ServiceDisputeEvidence e "
                                + "where e.disputa.id = :disputeId order by e.criadaEm, e.id",
                        Object[].class)
                .setParameter("disputeId", disputeId)
                .getResultList();
    }

    public ServiceDisputeEvidence buscarEvidencia(Long disputeId, Long evidenceId) {
        return getEntityManager().createQuery(
                        "select e from ServiceDisputeEvidence e join fetch e.enviadoPor "
                                + "where e.disputa.id = :disputeId and e.id = :evidenceId",
                        ServiceDisputeEvidence.class)
                .setParameter("disputeId", disputeId)
                .setParameter("evidenceId", evidenceId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public int excluirEvidenciasEncerradasAte(LocalDateTime cutoff) {
        return getEntityManager().createQuery(
                        "delete from ServiceDisputeEvidence e "
                                + "where e.disputa.encerradaEm is not null "
                                + "and e.disputa.encerradaEm <= :cutoff")
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }
}
