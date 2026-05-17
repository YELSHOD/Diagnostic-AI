package com.yelshod.diagnosticserviceai.runtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "runtime_targets")
public class RuntimeTargetEntity {

    @Id
    private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RuntimeTargetType type;

    @Column(length = 255)
    private String host;

    private Integer port;

    @Column(name = "health_url", length = 500)
    private String healthUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_source_type", nullable = false, length = 40)
    private LogSourceType logSourceType;

    @Column(name = "log_source_ref", length = 1000)
    private String logSourceRef;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
