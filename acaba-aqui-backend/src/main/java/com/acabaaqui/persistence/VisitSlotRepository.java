package com.acabaaqui.persistence;

import com.acabaaqui.model.VisitSlot;
import com.acabaaqui.model.VisitSlotStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.List;

@ApplicationScoped
public class VisitSlotRepository implements PanacheRepository<VisitSlot> {

    public VisitSlot buscarPorId(Long id) {
        return findById(id);
    }

    public VisitSlot buscarComBloqueio(Long id) {
        return findById(id, LockModeType.PESSIMISTIC_WRITE);
    }

    public List<VisitSlot> listarPorSolicitacao(Long requestId) {
        return list("select v from VisitSlot v left join fetch v.prestador "
                + "left join fetch v.prestadorConfirmado "
                + "where v.solicitacao.id = ?1 order by v.inicioLocal, v.id", requestId);
    }

    public List<VisitSlot> listarAtivasPorPrestador(Long requestId, Integer providerId) {
        return list("solicitacao.id = ?1 and prestador.id = ?2 "
                        + "and status in (?3, ?4)",
                requestId, providerId, VisitSlotStatus.RESERVADA, VisitSlotStatus.CONFIRMADA);
    }
}
