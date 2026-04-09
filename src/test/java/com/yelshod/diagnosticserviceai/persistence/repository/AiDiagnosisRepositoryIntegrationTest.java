package com.yelshod.diagnosticserviceai.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.yelshod.diagnosticserviceai.persistence.entity.AiDiagnosisEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.ClusterEntity;
import com.yelshod.diagnosticserviceai.support.PostgresIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AiDiagnosisRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private AiDiagnosisRepository aiDiagnosisRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @BeforeEach
    void cleanDatabase() {
        aiDiagnosisRepository.deleteAll();
        incidentRepository.deleteAll();
        clusterRepository.deleteAll();
    }

    @Test
    void persistsDiagnosisJsonByClusterKey() {
        clusterRepository.save(ClusterEntity.builder()
                .clusterKey("cluster-1")
                .service("orders")
                .title("IllegalStateException")
                .severity("high")
                .firstSeen(Instant.parse("2026-04-09T10:00:00Z"))
                .lastSeen(Instant.parse("2026-04-09T10:00:00Z"))
                .count(1)
                .build());

        aiDiagnosisRepository.save(AiDiagnosisEntity.builder()
                .clusterKey("cluster-1")
                .model("gemini-2.5-flash")
                .promptVersion("v1")
                .diagnosisJson("{\"severity\":\"high\"}")
                .createdAt(Instant.parse("2026-04-09T10:00:00Z"))
                .build());

        assertThat(aiDiagnosisRepository.findById("cluster-1")).isPresent();
    }
}
