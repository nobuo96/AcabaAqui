package com.acabaaqui.persistence;

import com.acabaaqui.model.PendingEmailRegistration;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PendingEmailRegistrationRepository implements PanacheRepository<PendingEmailRegistration> {

    public PendingEmailRegistration buscarPorEmailComLock(String email) {
        return find("email", email).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }

    @Transactional
    public void removerExpiradosAntesDe(java.time.LocalDateTime limite) {
        delete("expiraEm < ?1", limite);
    }

    @Transactional
    public void salvar(PendingEmailRegistration registration) {
        persist(registration);
    }

    @Transactional
    public void remover(PendingEmailRegistration registration) {
        delete(registration);
    }
}