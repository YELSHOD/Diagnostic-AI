package com.yelshod.diagnosticserviceai.logsource;

import com.yelshod.diagnosticserviceai.docker.DockerLogLine;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetDto;
import java.util.function.Consumer;

public interface LogSource {

    boolean supports(RuntimeTargetDto target);

    LogSourceSession stream(RuntimeTargetDto target, Consumer<DockerLogLine> consumer);
}
