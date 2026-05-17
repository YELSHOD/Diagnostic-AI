package com.yelshod.diagnosticserviceai.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "log_events")
public class LogEventEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "runtime_target_id", nullable = false)
    private UUID runtimeTargetId;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false, length = 20)
    private String level;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "stacktrace", columnDefinition = "text")
    private String stacktrace;

    @Column(length = 80)
    private String environment;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
