package com.yelshod.diagnosticserviceai.logsource;

import static org.assertj.core.api.Assertions.assertThat;

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
    void streamsNewLinesFromFileTailSource() throws Exception {
        Path file = Files.createTempFile("runtime-target", ".log");
        Files.writeString(file, "first\n");
        List<String> seen = new CopyOnWriteArrayList<>();
        FileTailLogSource fileTailLogSource = new FileTailLogSource();
        RuntimeTargetDto target = new RuntimeTargetDto(
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

        try (LogSourceSession ignored = fileTailLogSource.stream(target, line -> seen.add(line.line()))) {
            Files.writeString(file, "second\n", StandardOpenOption.APPEND);
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(seen).contains("second"));
        }
    }
}
