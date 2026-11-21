package com.acabaaqui.persistence;

import com.acabaaqui.model.ServiceWarranty;
import com.acabaaqui.model.ServiceWarrantyStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ServiceWarrantyRepository implements PanacheRepository<ServiceWarranty> {

    public ServiceWarranty buscarPorOrcamento(Long quoteId) {
        return find("select w from ServiceWarranty w join fetch w.orcamento q "
                        + "join fetch q.solicitacao r join fetch r.usuario "
                        + "join fetch q.prestador where q.id = ?1 order by w.id desc",
                quoteId).firstResult();
    }

    public ServiceWarranty buscarPorOrcamentoComBloqueio(Long quoteId) {
        return find("orcamento.id = ?1 order by id desc", quoteId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }

    public ServiceWarranty buscarPorIdComBloqueio(Long warrantyId) {
        return find("id = ?1", warrantyId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }

    public ServiceWarranty buscarDetalhe(Long warrantyId) {
        return find("select w from ServiceWarranty w join fetch w.orcamento q "
                        + "join fetch q.solicitacao r join fetch r.usuario "
                        + "join fetch q.prestador where w.id = ?1",
                warrantyId).firstResult();
    }

    public List<Long> listarChamadosVencidos(LocalDateTime now) {
        return getEntityManager().createQuery(
                        "select w.id from ServiceWarranty w "
                                + "where w.status = :status and w.correcaoAte <= :now "
                                + "order by w.correcaoAte, w.id",
                        Long.class)
                .setParameter("status", ServiceWarrantyStatus.ACIONADA)
                .setParameter("now", now)
                .getResultList();
    }

    public List<Long> listarReembolsosPendentes() {
        return getEntityManager().createQuery(
                        "select w.id from ServiceWarranty w where w.status = :status order by w.id",
                        Long.class)
                .setParameter("status", ServiceWarrantyStatus.REEMBOLSO_PENDENTE)
                .getResultList();
    }
}
