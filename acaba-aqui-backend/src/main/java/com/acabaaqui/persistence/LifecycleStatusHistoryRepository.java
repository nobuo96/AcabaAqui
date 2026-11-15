package com.acabaaqui.persistence;

import com.acabaaqui.model.LifecycleStatusHistory;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LifecycleStatusHistoryRepository implements PanacheRepository<LifecycleStatusHistory> {
}
