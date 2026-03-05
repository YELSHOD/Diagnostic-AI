package com.yelshod.diagnosticserviceai.api;

import com.yelshod.diagnosticserviceai.docker.DockerContainerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final DockerContainerService dockerContainerService;

    @GetMapping
    public List<ProjectContainerDto> projects() {
        return dockerContainerService.listDemoProjectContainers();
    }
}
