package com.yelshod.diagnosticserviceai.logsource;

import com.yelshod.diagnosticserviceai.docker.DockerLogLine;
import com.yelshod.diagnosticserviceai.persistence.entity.LogEventEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.LogEventRepository;
import com.yelshod.diagnosticserviceai.runtime.LogSourceType;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetDto;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class HttpIngestLogSource implements LogSource {

    private final LogEventRepository logEventRepository;

    @Override
    public boolean supports(RuntimeTargetDto target) {
        return target.logSourceType() == LogSourceType.HTTP_INGEST;
    }

    @Override
    public LogSourceSession stream(RuntimeTargetDto target, Consumer<DockerLogLine> consumer) {
        UUID runtimeTargetId = UUID.fromString(target.id());
        Instant[] cursor = {emitInitialHistory(runtimeTargetId, target, consumer)};

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "http-ingest-log-stream-" + target.id());
            thread.setDaemon(true);
            return thread;
        });
        ScheduledFuture<?> future = executor.scheduleWithFixedDelay(
                () -> cursor[0] = emitNewEvents(runtimeTargetId, target, cursor[0], consumer),
                500,
                500,
                TimeUnit.MILLISECONDS
        );

        return () -> {
            future.cancel(true);
            executor.shutdownNow();
        };
    }

    private Instant emitInitialHistory(UUID runtimeTargetId, RuntimeTargetDto target, Consumer<DockerLogLine> consumer) {
        List<LogEventEntity> history = logEventRepository.findTop200ByRuntimeTargetIdOrderByEventTimeDesc(runtimeTargetId)
                .stream()
                .sorted(Comparator.comparing(LogEventEntity::getCreatedAt))
                .toList();
        history.forEach(event -> consumer.accept(toLine(target, event)));
        return history.stream()
                .map(LogEventEntity::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElse(Instant.EPOCH);
    }

    private Instant emitNewEvents(UUID runtimeTargetId, RuntimeTargetDto target, Instant cursor, Consumer<DockerLogLine> consumer) {
        Instant nextCursor = cursor;
        try {
            List<LogEventEntity> events = logEventRepository.findByRuntimeTargetIdAndCreatedAtAfterOrderByCreatedAtAsc(runtimeTargetId, cursor);
            for (LogEventEntity event : events) {
                consumer.accept(toLine(target, event));
                if (event.getCreatedAt().isAfter(nextCursor)) {
                    nextCursor = event.getCreatedAt();
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Failed polling HTTP ingest logs runtimeTargetId={}", runtimeTargetId, ex);
        }
        return nextCursor;
    }

    private DockerLogLine toLine(RuntimeTargetDto target, LogEventEntity event) {
        StringBuilder line = new StringBuilder()
                .append(event.getEventTime())
                .append(' ')
                .append(event.getLevel())
                .append(' ')
                .append(event.getMessage());
        if (event.getStacktrace() != null && !event.getStacktrace().isBlank()) {
            line.append('\n').append(event.getStacktrace().strip());
        }
        return new DockerLogLine(target.name(), line.toString());
    }
}
