package com.acabaaqui.persistence;

import com.acabaaqui.model.ServicePayment;
import com.acabaaqui.model.PaymentStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.List;

@ApplicationScoped
public class ServicePaymentRepository implements PanacheRepository<ServicePayment> {

    public ServicePayment buscarPorId(Long paymentId) {
        return find("select p from ServicePayment p join fetch p.orcamento q "
                        + "join fetch q.solicitacao r join fetch r.usuario "
                        + "where p.id = ?1",
                paymentId).firstResult();
    }

    public ServicePayment buscarPorOrcamento(Long quoteId) {
        return find("select p from ServicePayment p join fetch p.orcamento q "
                        + "join fetch q.solicitacao r join fetch r.usuario "
                        + "where q.id = ?1",
                quoteId).firstResult();
    }

    public ServicePayment buscarPorIdComBloqueio(Long paymentId) {
        return find("id = ?1", paymentId).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }

    public ServicePayment buscarPorOrcamentoComBloqueio(Long quoteId) {
        return find("orcamento.id = ?1", quoteId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }

    public ServicePayment buscarPorOrderIdComBloqueio(String orderId) {
        return find("mercadoPagoOrderId = ?1", orderId)
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }

    public List<Long> listarIdsAguardando() {
        return getEntityManager().createQuery(
                        "select p.id from ServicePayment p "
                                + "where p.status in (:statuses) "
                                + "and p.mercadoPagoOrderId is not null order by p.id",
                        Long.class)
                .setParameter("statuses", List.of(
                        PaymentStatus.AGUARDANDO_PAGAMENTO,
                        PaymentStatus.REEMBOLSO_PENDENTE))
                .getResultList();
    }
}
