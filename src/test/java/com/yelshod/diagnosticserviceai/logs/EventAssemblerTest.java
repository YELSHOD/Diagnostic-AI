package com.yelshod.diagnosticserviceai.logs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EventAssemblerTest {

    @Test
    void completesBufferedEventWhenNextTimestampedLineStartsANewLogEntry() {
        EventAssemblyTimeoutScheduler scheduler = (service, action) -> () -> { };
        EventAssembler assembler = new EventAssembler(scheduler);
        AtomicReference<ErrorEvent> timedOut = new AtomicReference<>();

        assembler.process(new ParsedLogLine(
                Instant.parse("2026-04-09T10:00:00Z"),
                "ERROR",
                "2026-04-09T10:00:00Z ERROR java.lang.IllegalStateException: boom",
                "trace-1",
                "orders",
                "2026-04-09T10:00:00Z ERROR java.lang.IllegalStateException: boom"), timedOut::set);

        assembler.process(new ParsedLogLine(
                Instant.parse("2026-04-09T10:00:01Z"),
                null,
                "at com.example.OrdersService.placeOrder(OrdersService.java:42)",
                "trace-1",
                "orders",
                "at com.example.OrdersService.placeOrder(OrdersService.java:42)"), timedOut::set);

        Optional<ErrorEvent> completed = assembler.process(new ParsedLogLine(
                Instant.parse("2026-04-09T10:00:02Z"),
                "INFO",
                "2026-04-09T10:00:02Z INFO next message",
                "trace-2",
                "orders",
                "2026-04-09T10:00:02Z INFO next message"), timedOut::set);

        assertThat(completed).isPresent();
        assertThat(completed.get().exceptionType()).isEqualTo("java.lang.IllegalStateException");
        assertThat(completed.get().stackFrames())
                .contains("at com.example.OrdersService.placeOrder(OrdersService.java:42)");
    }
}
