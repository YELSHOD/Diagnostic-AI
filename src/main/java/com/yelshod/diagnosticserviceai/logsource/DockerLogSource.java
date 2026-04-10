package com.yelshod.diagnosticserviceai.logsource;

import com.yelshod.diagnosticserviceai.docker.DockerLogsService;
import com.yelshod.diagnosticserviceai.runtime.LogSourceType;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetDto;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DockerLogSource implements LogSource {

    private final DockerLogsService dockerLogsService;

    @Override
    public boolean supports(RuntimeTargetDto target) {
        return target.logSourceType() == LogSourceType.DOCKER;
    }

    @Override
    public LogSourceSession stream(RuntimeTargetDto target, Consumer<com.yelshod.diagnosticserviceai.docker.DockerLogLine> consumer) {
        return dockerLogsService.streamLogs(target.logSourceRef(), consumer)::close;
    }
}
