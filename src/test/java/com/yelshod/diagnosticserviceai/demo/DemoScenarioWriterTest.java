package com.yelshod.diagnosticserviceai.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DemoScenarioWriterTest {

    @Test
    void appendsBusinessLogLinesToTheConfiguredDemoFile() throws Exception {
        Path file = Files.createTempFile("orders-demo", ".log");
        DemoScenarioWriter writer = new DemoScenarioWriter();

        writer.append(file, new DemoScenarioLine(
                Instant.parse("2026-04-13T06:30:00Z"),
                "INFO",
                "orders-demo",
                "Order created orderId=ORD-20260413-1001 customer=\"Aruzhan S.\" phone=\"+7 700 *** 12 34\""
        ));

        assertThat(Files.readString(file)).contains("Order created orderId=ORD-20260413-1001");
    }
}
