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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DockerLogsService {

    private final DockerClient dockerClient;
    private final AppProperties appProperties;
    private final DockerFrameLogSplitter dockerFrameLogSplitter;

    public DockerLogSession streamLogs(String containerId, Consumer<DockerLogLine> consumer) {
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
    }

    private String resolveServiceName(String containerId) {
        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
        Map<String, String> labels = response.getConfig() == null ? Map.of() : response.getConfig().getLabels();
        if (labels == null || labels.isEmpty()) {
            return response.getName().replaceFirst("^/", "");
        }
        return labels.getOrDefault("com.docker.compose.service", response.getName().replaceFirst("^/", ""));
    }
}
