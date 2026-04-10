package com.yelshod.diagnosticserviceai.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuntimeTargetServiceTest {

    @Test
    void mergesTargetsFromAllDiscoveryProvidersInStableOrder() {
        RuntimeTargetDiscoveryService dockerDiscovery = () -> List.of(
                new RuntimeTargetDto(
                        "docker-orders",
                        "orders",
                        RuntimeTargetType.DOCKER_CONTAINER,
                        RuntimeTargetStatus.UP,
                        "localhost",
                        8081,
                        null,
                        LogSourceType.DOCKER,
                        "docker-orders",
                        Map.of("source", "docker")));
        RuntimeTargetDiscoveryService localDiscovery = () -> List.of(
                new RuntimeTargetDto(
                        "local-api",
                        "api",
                        RuntimeTargetType.LOCAL_SERVICE,
                        RuntimeTargetStatus.UNKNOWN,
                        "127.0.0.1",
                        8080,
                        null,
                        LogSourceType.FILE_TAIL,
                        "/tmp/api.log",
                        Map.of("source", "db")));

        RuntimeTargetService service = new RuntimeTargetService(List.of(localDiscovery, dockerDiscovery));

        assertThat(service.listRuntimeTargets())
                .extracting(RuntimeTargetDto::id)
                .containsExactly("docker-orders", "local-api");
    }
}
