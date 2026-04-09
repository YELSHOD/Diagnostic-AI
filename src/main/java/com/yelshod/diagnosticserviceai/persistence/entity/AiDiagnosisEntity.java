package com.yelshod.diagnosticserviceai.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_diagnosis")
public class AiDiagnosisEntity {

    @Id
    @Column(name = "cluster_key", nullable = false, length = 512)
    private String clusterKey;

    @Column(nullable = false)
    private String model;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diagnosis_json", nullable = false, columnDefinition = "jsonb")
    private String diagnosisJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
