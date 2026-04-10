package com.yelshod.diagnosticserviceai.runtime;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.time.Instant;
import java.util.Arrays;
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
public class DockerRuntimeDiscoveryService implements RuntimeTargetDiscoveryService {

    private final DockerClient dockerClient;
    private final AppProperties appProperties;

    @Override
    public List<RuntimeTargetDto> discover() {
        try {
            String labelName = appProperties.docker().projectLabel();
            String labelValue = appProperties.docker().projectLabelValue();

            return dockerClient.listContainersCmd().withShowAll(true).exec().stream()
                    .filter(container -> {
                        Map<String, String> labels = container.getLabels();
                        return labels != null && labelValue.equals(labels.get(labelName));
                    })
                    .map(this::toDto)
                    .collect(Collectors.toList());
        } catch (RuntimeException ex) {
            log.error("Docker runtime discovery failed", ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Docker daemon is unavailable", ex);
        }
    }

    private RuntimeTargetDto toDto(Container container) {
        String name = container.getNames() == null ? "unknown" : Arrays.stream(container.getNames())
                .findFirst()
                .orElse("unknown")
                .replaceFirst("^/", "");

        RuntimeTargetStatus status = container.getStatus() != null && container.getStatus().startsWith("Up")
                ? RuntimeTargetStatus.UP
                : RuntimeTargetStatus.DOWN;
        Map<String, String> labels = container.getLabels() == null ? Map.of() : Map.copyOf(container.getLabels());

        return new RuntimeTargetDto(
                container.getId(),
                name,
                RuntimeTargetType.DOCKER_CONTAINER,
                status,
                "localhost",
                null,
                null,
                LogSourceType.DOCKER,
                container.getId(),
                Map.of(
                        "image", container.getImage() == null ? "unknown" : container.getImage(),
                        "dockerStatus", container.getStatus() == null ? "unknown" : container.getStatus(),
                        "createdAt", Instant.ofEpochSecond(container.getCreated()).toString(),
                        "env", labels.getOrDefault(appProperties.docker().projectLabel(), "")
                )
        );
    }
}
