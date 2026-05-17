package com.yelshod.diagnosticserviceai.project;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingest-projects")
@RequiredArgsConstructor
public class IngestProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectDto> projects() {
        return projectService.listProjects();
    }

    @GetMapping("/default")
    public ProjectDto defaultProject() {
        return projectService.getOrCreateDefaultProject();
    }

    @PostMapping("/default")
    public ProjectDto generateDefaultProject() {
        return projectService.getOrCreateDefaultProject();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }
}
