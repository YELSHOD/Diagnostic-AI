package com.yelshod.diagnosticserviceai.persistence.repository;

import com.yelshod.diagnosticserviceai.persistence.entity.LogEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEventRepository extends JpaRepository<LogEventEntity, UUID> {

    List<LogEventEntity> findTop200ByRuntimeTargetIdOrderByEventTimeDesc(UUID runtimeTargetId);

    List<LogEventEntity> findByRuntimeTargetIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID runtimeTargetId, Instant createdAt);
}
