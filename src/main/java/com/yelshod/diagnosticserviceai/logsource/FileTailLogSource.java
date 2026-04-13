package com.yelshod.diagnosticserviceai.logsource;

import com.yelshod.diagnosticserviceai.docker.DockerLogLine;
import com.yelshod.diagnosticserviceai.runtime.LogSourceType;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetDto;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Slf4j
public class FileTailLogSource implements LogSource {

    private static final int INITIAL_HISTORY_LINES = 200;

    @Override
    public boolean supports(RuntimeTargetDto target) {
        return target.logSourceType() == LogSourceType.FILE_TAIL;
    }

    @Override
    public LogSourceSession stream(RuntimeTargetDto target, Consumer<DockerLogLine> consumer) {
        String pathRef = target.logSourceRef();
        if (pathRef == null || pathRef.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File tail target requires logSourceRef");
        }

        Path path = Path.of(pathRef);

        try {
            ensureFileExists(path);
            emitInitialHistory(target, path, consumer);
            RandomAccessFile file = new RandomAccessFile(path.toFile(), "r");
            file.seek(file.length());

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "file-tail-" + target.id());
                thread.setDaemon(true);
                return thread;
            });
            ScheduledFuture<?> future = executor.scheduleWithFixedDelay(() -> readNewLines(target, file, consumer), 0, 200, TimeUnit.MILLISECONDS);

            return () -> {
                future.cancel(true);
                executor.shutdownNow();
                try {
                    file.close();
                } catch (IOException ignored) {
                }
            };
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to tail log file: " + pathRef, ex);
        }
    }

    private void ensureFileExists(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
    }

    private void emitInitialHistory(RuntimeTargetDto target, Path path, Consumer<DockerLogLine> consumer) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        int start = Math.max(0, lines.size() - INITIAL_HISTORY_LINES);
        for (int index = start; index < lines.size(); index++) {
            consumer.accept(new DockerLogLine(target.name(), lines.get(index)));
        }
    }

    private void readNewLines(RuntimeTargetDto target, RandomAccessFile file, Consumer<DockerLogLine> consumer) {
        try {
            String line;
            while ((line = file.readLine()) != null) {
                consumer.accept(new DockerLogLine(target.name(), new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)));
            }
        } catch (IOException ex) {
            log.warn("Failed reading tailed log file for {}", target.id(), ex);
        }
    }
}
