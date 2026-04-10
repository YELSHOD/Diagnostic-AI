package com.yelshod.diagnosticserviceai.runtime;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuntimeTargetRepository extends JpaRepository<RuntimeTargetEntity, UUID> {

    List<RuntimeTargetEntity> findAllByEnabledTrueOrderByNameAsc();
}
