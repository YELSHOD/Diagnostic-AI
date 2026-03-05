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
@Table(name = "incidents")
public class IncidentEntity {

    @Id
    private UUID id;

    @Column(name = "cluster_key", nullable = false, length = 512)
    private String clusterKey;

    @Column(nullable = false)
    private String service;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "exception_type")
    private String exceptionType;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "top_frame", columnDefinition = "text")
    private String topFrame;

    @Column(name = "stacktrace", columnDefinition = "text")
    private String stacktrace;

    @Column(name = "context", columnDefinition = "text")
    private String context;
}
