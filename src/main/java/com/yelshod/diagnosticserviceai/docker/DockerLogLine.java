package com.yelshod.diagnosticserviceai.docker;

public record DockerLogLine(
        String service,
        String line
) {
}
