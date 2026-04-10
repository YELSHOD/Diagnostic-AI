package com.yelshod.diagnosticserviceai.logsource;

import com.yelshod.diagnosticserviceai.docker.DockerLogLine;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetDto;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class LogSourceRouter {

    private final List<LogSource> logSources;

    public LogSourceSession open(RuntimeTargetDto target, Consumer<DockerLogLine> consumer) {
        return logSources.stream()
                .filter(source -> source.supports(target))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported log source for target " + target.id()))
                .stream(target, consumer);
    }
}
