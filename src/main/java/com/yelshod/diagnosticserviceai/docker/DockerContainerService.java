package com.yelshod.diagnosticserviceai.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.yelshod.diagnosticserviceai.api.ProjectContainerDto;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.net.SocketException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class DockerContainerService {

    private final DockerClient dockerClient;
    private final AppProperties appProperties;

    public List<ProjectContainerDto> listDemoProjectContainers() {
        try {
            String labelName = appProperties.docker().projectLabel();
            String labelValue = appProperties.docker().projectLabelValue();

            List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
            return containers.stream()
                    .filter(container -> {
                        Map<String, String> labels = container.getLabels();
                        return labels != null && labelValue.equals(labels.get(labelName));
                    })
                    .map(this::toDto)
                    .collect(Collectors.toList());
        } catch (RuntimeException ex) {
            if (isMissingDockerSocket(ex)) {
                log.warn("Docker container discovery skipped because Docker socket is unavailable");
                return List.of();
            }
            log.error("Docker container discovery failed", ex);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Docker daemon is unavailable for container discovery",
                    ex
            );
        }
    }

    private boolean isMissingDockerSocket(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketException socketException
                    && socketException.getMessage() != null
                    && socketException.getMessage().contains("No such file or directory")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ProjectContainerDto toDto(Container container) {
        String name = container.getNames() == null ? "unknown" : Arrays.stream(container.getNames())
                .findFirst()
                .orElse("unknown")
                .replaceFirst("^/", "");
        return new ProjectContainerDto(
                container.getId(),
                name,
                container.getImage(),
                container.getStatus(),
                Instant.ofEpochSecond(container.getCreated()),
                container.getLabels() == null ? Collections.emptyMap() : Map.copyOf(container.getLabels())
        );
    }
}
