package com.yelshod.diagnosticserviceai.logsource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.yelshod.diagnosticserviceai.runtime.LogSourceType;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetDto;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetStatus;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

class FileTailLogSourceTest {

    @Test
    void createsMissingLogFileInsteadOfFailingStreamOpen() throws Exception {
        Path directory = Files.createTempDirectory("runtime-target-dir");
        Path file = directory.resolve("nested/diagnosticserviceai.log");
        FileTailLogSource fileTailLogSource = new FileTailLogSource();
        RuntimeTargetDto target = target(file);

        assertThatCode(() -> {
            try (LogSourceSession ignored = fileTailLogSource.stream(target, line -> { })) {
                assertThat(Files.exists(file)).isTrue();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void replaysExistingLinesWhenStreamStarts() throws Exception {
        Path file = Files.createTempFile("runtime-target-history", ".log");
        Files.writeString(file, "first\nsecond\n");
        List<String> seen = new CopyOnWriteArrayList<>();
        FileTailLogSource fileTailLogSource = new FileTailLogSource();

        try (LogSourceSession ignored = fileTailLogSource.stream(target(file), line -> seen.add(line.line()))) {
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                    assertThat(seen).contains("first", "second"));
        }
    }

    @Test
    void streamsNewLinesFromFileTailSource() throws Exception {
        Path file = Files.createTempFile("runtime-target", ".log");
        Files.writeString(file, "first\n");
        List<String> seen = new CopyOnWriteArrayList<>();
        FileTailLogSource fileTailLogSource = new FileTailLogSource();
        RuntimeTargetDto target = target(file);

        try (LogSourceSession ignored = fileTailLogSource.stream(target, line -> seen.add(line.line()))) {
            Files.writeString(file, "second\n", StandardOpenOption.APPEND);
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(seen).contains("second"));
        }
    }

    private RuntimeTargetDto target(Path file) {
        return new RuntimeTargetDto(
                "local:front",
                "diagnostic-ai-front",
                RuntimeTargetType.LOCAL_SERVICE,
                RuntimeTargetStatus.UP,
                "localhost",
                5173,
                "http://localhost:5173",
                LogSourceType.FILE_TAIL,
                file.toString(),
                Map.of()
        );
    }
}
