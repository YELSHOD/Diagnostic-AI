package com.yelshod.diagnosticserviceai.docker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class DockerContainerServiceTest {

    @Test
    void returnsServiceUnavailableWhenDockerDaemonCannotBeReached() {
        DockerClient dockerClient = mock(DockerClient.class);
        ListContainersCmd listContainersCmd = mock(ListContainersCmd.class);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(true)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenThrow(new RuntimeException("boom"));

        var service = new DockerContainerService(dockerClient, appProperties());

        assertThatThrownBy(service::listDemoProjectContainers)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private AppProperties appProperties() {
        return new AppProperties(new AppProperties.Docker("ai.project.env", "demo", 200), null);
    }
}
