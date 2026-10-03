package com.acabaaqui.persistence;

import com.acabaaqui.model.PasswordResetChallenge;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@ApplicationScoped
public class PasswordResetChallengeRepository implements PanacheRepository<PasswordResetChallenge> {

    public PasswordResetChallenge buscarPorUsuarioComLock(Integer usuarioId) {
        return find("usuarioId", usuarioId).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }

    @Transactional
    public void salvar(PasswordResetChallenge challenge) {
        persist(challenge);
    }

    @Transactional
    public void remover(PasswordResetChallenge challenge) {
        delete(challenge);
    }

    @Transactional
    public void removerExpiradosAntesDe(LocalDateTime limite) {
        delete("expiraEm < ?1", limite);
    }
}