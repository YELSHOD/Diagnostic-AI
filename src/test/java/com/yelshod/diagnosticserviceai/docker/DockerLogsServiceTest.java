package com.yelshod.diagnosticserviceai.docker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class DockerLogsServiceTest {

    @Test
    void returnsServiceUnavailableWhenDockerDaemonCannotInspectContainer() {
        DockerClient dockerClient = mock(DockerClient.class);
        InspectContainerCmd inspectContainerCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("abc")).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenThrow(new RuntimeException("boom"));

        var service = new DockerLogsService(
                dockerClient,
                new AppProperties(new AppProperties.Docker("ai.project.env", "demo", 200), null),
                new DockerFrameLogSplitter()
        );

        assertThatThrownBy(() -> service.streamLogs("abc", line -> { }))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
