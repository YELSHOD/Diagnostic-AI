package com.yelshod.diagnosticserviceai.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.yelshod.diagnosticserviceai.persistence.entity.ClusterEntity;
import com.yelshod.diagnosticserviceai.support.PostgresIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ClusterRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AiDiagnosisRepository aiDiagnosisRepository;

    @BeforeEach
    void cleanDatabase() {
        aiDiagnosisRepository.deleteAll();
        incidentRepository.deleteAll();
        clusterRepository.deleteAll();
    }

    @Test
    void persistsAndLoadsClusterByKey() {
        ClusterEntity cluster = ClusterEntity.builder()
                .clusterKey("cluster-1")
                .service("orders")
                .title("IllegalStateException")
                .severity("high")
                .firstSeen(Instant.parse("2026-04-09T10:00:00Z"))
                .lastSeen(Instant.parse("2026-04-09T10:05:00Z"))
                .count(3)
                .build();

        clusterRepository.save(cluster);

        assertThat(clusterRepository.findById("cluster-1"))
                .get()
                .extracting(ClusterEntity::getService, ClusterEntity::getCount)
                .containsExactly("orders", 3L);
    }
}
