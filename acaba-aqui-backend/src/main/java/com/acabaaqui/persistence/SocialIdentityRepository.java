package com.acabaaqui.persistence;

import com.acabaaqui.model.SocialAuthProvider;
import com.acabaaqui.model.SocialIdentity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SocialIdentityRepository implements PanacheRepository<SocialIdentity> {

    public SocialIdentity buscar(SocialAuthProvider provedor, String subject) {
        return find("provedor = ?1 and subject = ?2", provedor, subject).firstResult();
    }

    @Transactional
    public void adicionar(SocialIdentity identity) {
        persist(identity);
    }
}
