package com.acabaaqui.persistence;

import com.acabaaqui.model.ServicePayout;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.List;

@ApplicationScoped
public class ServicePayoutRepository implements PanacheRepository<ServicePayout> {

    public ServicePayout buscarPorOrcamentoComBloqueio(Long quoteId) {
        return find("execucao.id = ?1", quoteId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }

    public ServicePayout buscarPorIdComBloqueio(Long payoutId) {
        return find("id = ?1", payoutId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }

    public ServicePayout buscarDetalhe(Long payoutId) {
        return find("select p from ServicePayout p join fetch p.execucao e "
                        + "join fetch e.orcamento q join fetch q.prestador "
                        + "join fetch q.solicitacao r join fetch r.servico "
                        + "where p.id = ?1",
                payoutId).firstResult();
    }

    public List<ServicePayout> listarTodos() {
        return find("select p from ServicePayout p join fetch p.execucao e "
                        + "join fetch e.orcamento q join fetch q.prestador "
                        + "join fetch q.solicitacao r join fetch r.servico "
                        + "order by case when p.status = 'PENDENTE' then 0 else 1 end, "
                        + "p.elegivelEm, p.id")
                .list();
    }

    public boolean existeReferenciaTransferencia(String reference) {
        return count("referenciaTransferencia = ?1", reference) > 0;
    }
}
