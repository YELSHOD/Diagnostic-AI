package com.yelshod.diagnosticserviceai.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClusterKeyFactoryTest {

    private final ClusterKeyFactory clusterKeyFactory = new ClusterKeyFactory();

    @Test
    void normalizesVolatileValuesBeforeHashingMessage() {
        String keyA = clusterKeyFactory.build(
                "IllegalStateException",
                "OrdersService.placeOrder",
                "Order 123 failed for request 550e8400-e29b-41d4-a716-446655440000");
        String keyB = clusterKeyFactory.build(
                "IllegalStateException",
                "OrdersService.placeOrder",
                "Order 456 failed for request 123e4567-e89b-12d3-a456-426614174000");

        assertThat(keyA).isEqualTo(keyB);
    }
}
