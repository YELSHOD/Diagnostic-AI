package com.yelshod.diagnosticserviceai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.net.SocketException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DockerRuntimeDiscoveryServiceTest {

    @Test
    void mapsDockerContainersIntoRuntimeTargets() {
        DockerClient dockerClient = mock(DockerClient.class);
        ListContainersCmd listContainersCmd = mock(ListContainersCmd.class);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(true)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(java.util.List.of(container()));

        var discovery = new DockerRuntimeDiscoveryService(dockerClient, appProperties());

        assertThat(discovery.discover())
                .singleElement()
                .satisfies(target -> {
                    assertThat(target.id()).isEqualTo("container-1");
                    assertThat(target.type()).isEqualTo(RuntimeTargetType.DOCKER_CONTAINER);
                    assertThat(target.status()).isEqualTo(RuntimeTargetStatus.UP);
                    assertThat(target.logSourceType()).isEqualTo(LogSourceType.DOCKER);
                    assertThat(target.metadata()).containsEntry("image", "orders:latest");
                });
    }

    @Test
    void returnsEmptyListWhenDockerSocketIsUnavailable() throws Exception {
        DockerClient dockerClient = mock(DockerClient.class);
        ListContainersCmd listContainersCmd = mock(ListContainersCmd.class);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(true)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenThrow(new RuntimeException(new SocketException("No such file or directory")));

        var discovery = new DockerRuntimeDiscoveryService(dockerClient, appProperties());

        assertThat(discovery.discover()).isEmpty();
    }

    private Container container() {
        Container container = new Container();
        ReflectionTestUtils.setField(container, "id", "container-1");
        ReflectionTestUtils.setField(container, "names", new String[]{"/orders"});
        ReflectionTestUtils.setField(container, "image", "orders:latest");
        ReflectionTestUtils.setField(container, "status", "Up 5 minutes");
        ReflectionTestUtils.setField(container, "created", 1_744_267_600L);
        ReflectionTestUtils.setField(container, "labels", Map.of("ai.project.env", "demo"));
        return container;
    }

    private AppProperties appProperties() {
        return new AppProperties(new AppProperties.Docker("ai.project.env", "demo", 200), null, null);
    }
}
