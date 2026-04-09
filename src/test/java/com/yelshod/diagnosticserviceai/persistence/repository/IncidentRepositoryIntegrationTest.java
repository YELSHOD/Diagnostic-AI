package com.yelshod.diagnosticserviceai.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.yelshod.diagnosticserviceai.persistence.entity.ClusterEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.IncidentEntity;
import com.yelshod.diagnosticserviceai.support.PostgresIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IncidentRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private AiDiagnosisRepository aiDiagnosisRepository;

    @BeforeEach
    void cleanDatabase() {
        aiDiagnosisRepository.deleteAll();
        incidentRepository.deleteAll();
        clusterRepository.deleteAll();
    }

    @Test
    void returnsGroupedAnalyticsForSelectedService() {
        ClusterEntity cluster = ClusterEntity.builder()
                .clusterKey("cluster-1")
                .service("orders")
                .title("IllegalStateException")
                .severity("high")
                .firstSeen(Instant.parse("2026-04-09T10:00:00Z"))
                .lastSeen(Instant.parse("2026-04-09T10:00:00Z"))
                .count(1)
                .build();
        clusterRepository.save(cluster);

        incidentRepository.save(IncidentEntity.builder()
                .id(UUID.randomUUID())
                .clusterKey("cluster-1")
                .service("orders")
                .eventTime(Instant.parse("2026-04-09T10:15:20Z"))
                .exceptionType("IllegalStateException")
                .message("boom")
                .topFrame("OrdersService.placeOrder")
                .stacktrace("stack")
                .context("ctx")
                .build());

        var rows = incidentRepository.errorsPerMinute(
                Instant.parse("2026-04-09T10:00:00Z"),
                Instant.parse("2026-04-09T11:00:00Z"),
                "orders");

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getCount()).isEqualTo(1L);
    }
}
