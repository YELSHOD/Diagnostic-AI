package com.yelshod.diagnosticserviceai.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class DockerLogsService {

    private final DockerClient dockerClient;
    private final AppProperties appProperties;
    private final DockerFrameLogSplitter dockerFrameLogSplitter;

    public DockerLogSession streamLogs(String containerId, Consumer<DockerLogLine> consumer) {
        try {
            String service = resolveServiceName(containerId);
            LogContainerResultCallback callback = new LogContainerResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    if (frame.getStreamType() == StreamType.STDOUT || frame.getStreamType() == StreamType.STDERR) {
                        for (String line : dockerFrameLogSplitter.split(frame.getPayload())) {
                            consumer.accept(new DockerLogLine(service, line));
                        }
                    }
                    super.onNext(frame);
                }
            };

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .withTail(appProperties.docker().logsTail())
                    .exec(callback);

            return () -> {
                try {
                    callback.close();
                } catch (Exception ignored) {
                }
            };
        } catch (RuntimeException ex) {
            log.error("Docker log streaming failed for container {}", containerId, ex);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Docker daemon is unavailable for log streaming",
                    ex
            );
        }
    }

    private String resolveServiceName(String containerId) {
        try {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
            Map<String, String> labels = response.getConfig() == null ? Map.of() : response.getConfig().getLabels();
            if (labels == null || labels.isEmpty()) {
                return response.getName().replaceFirst("^/", "");
            }
            return labels.getOrDefault("com.docker.compose.service", response.getName().replaceFirst("^/", ""));
        } catch (RuntimeException ex) {
            log.error("Docker container inspection failed for {}", containerId, ex);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Docker daemon is unavailable for container inspection",
                    ex
            );
        }
    }
}
