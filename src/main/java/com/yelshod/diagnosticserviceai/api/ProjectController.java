package com.yelshod.diagnosticserviceai.api;

import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetService;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetType;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final RuntimeTargetService runtimeTargetService;

    @GetMapping
    public List<ProjectContainerDto> projects() {
        return runtimeTargetService.listRuntimeTargets().stream()
                .filter(target -> target.type() == RuntimeTargetType.DOCKER_CONTAINER)
                .map(target -> new ProjectContainerDto(
                        target.id(),
                        target.name(),
                        target.metadata().getOrDefault("image", "unknown"),
                        target.metadata().getOrDefault("dockerStatus", target.status().name()),
                        Instant.parse(target.metadata().getOrDefault("createdAt", Instant.EPOCH.toString())),
                        target.metadata()))
                .toList();
    }
}
