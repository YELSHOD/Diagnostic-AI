package com.yelshod.diagnosticserviceai.docker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.net.SocketException;
import org.junit.jupiter.api.Test;

class DockerContainerServiceTest {

    @Test
    void returnsEmptyListWhenDockerSocketIsUnavailable() throws Exception {
        DockerClient dockerClient = mock(DockerClient.class);
        ListContainersCmd listContainersCmd = mock(ListContainersCmd.class);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(true)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenThrow(new RuntimeException(new SocketException("No such file or directory")));

        var service = new DockerContainerService(dockerClient, appProperties());

        assertThat(service.listDemoProjectContainers()).isEmpty();
    }

    private AppProperties appProperties() {
        return new AppProperties(new AppProperties.Docker("ai.project.env", "demo", 200), null, null, null);
    }
}
