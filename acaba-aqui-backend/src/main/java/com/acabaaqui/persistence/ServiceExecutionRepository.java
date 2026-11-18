package com.acabaaqui.persistence;

import com.acabaaqui.model.ServiceExecution;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ServiceExecutionRepository implements PanacheRepository<ServiceExecution> {

    public ServiceExecution buscarPorOrcamentoComBloqueio(Long quoteId) {
        return find("id = ?1", quoteId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }

    public ServiceExecution buscarDetalhe(Long quoteId) {
        return find("select e from ServiceExecution e join fetch e.orcamento q "
                        + "join fetch q.prestador join fetch q.solicitacao r "
                        + "join fetch r.usuario join fetch r.servico where e.id = ?1",
                quoteId).firstResult();
    }

    public List<ServiceExecution> listarDoCliente(Integer userId) {
        return find("select e from ServiceExecution e join fetch e.orcamento q "
                        + "join fetch q.prestador join fetch q.solicitacao r "
                        + "join fetch r.servico where r.usuario.id = ?1 "
                        + "order by e.ultimaAtualizacao desc, e.id desc",
                userId).list();
    }

    public List<ServiceExecution> listarDoPrestador(Integer userId) {
        return find("select e from ServiceExecution e join fetch e.orcamento q "
                        + "join fetch q.solicitacao r join fetch r.usuario "
                        + "join fetch r.servico where q.prestador.id = ?1 "
                        + "order by e.ultimaAtualizacao desc, e.id desc",
                userId).list();
    }

    public List<Long> listarConcluidasAte(LocalDateTime cutoff) {
        return getEntityManager().createQuery(
                        "select e.id from ServiceExecution e "
                                + "where e.status = 'CONCLUIDA_PELO_PRESTADOR' "
                                + "and e.aprovarAte <= :cutoff "
                                + "and not exists (select p.id from ServicePayout p where p.execucao.id = e.id) "
                                + "order by e.aprovarAte, e.id",
                        Long.class)
                .setParameter("cutoff", cutoff)
                .getResultList();
    }
}
