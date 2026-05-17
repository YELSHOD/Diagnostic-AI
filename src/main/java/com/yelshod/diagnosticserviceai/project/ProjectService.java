package com.yelshod.diagnosticserviceai.project;

import com.yelshod.diagnosticserviceai.persistence.entity.ProjectEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.ProjectRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ProjectRepository projectRepository;
    private final Clock clock;

    public List<ProjectDto> listProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    public ProjectDto getOrCreateDefaultProject() {
        return projectRepository.findFirstByOrderByCreatedAtAsc()
                .map(this::toDto)
                .orElseGet(() -> createProject(new CreateProjectRequest("Default Diagnostic Project")));
    }

    public ProjectDto createProject(CreateProjectRequest request) {
        ProjectEntity entity = ProjectEntity.builder()
                .id(UUID.randomUUID())
                .name(request.name().trim())
                .projectKey(generateProjectKey())
                .createdAt(clock.instant())
                .build();
        return toDto(projectRepository.save(entity));
    }

    private String generateProjectKey() {
        String key;
        do {
            byte[] bytes = new byte[18];
            RANDOM.nextBytes(bytes);
            key = "prj_" + HexFormat.of().formatHex(bytes);
        } while (projectRepository.existsByProjectKey(key));
        return key;
    }

    private ProjectDto toDto(ProjectEntity entity) {
        return new ProjectDto(entity.getId(), entity.getName(), entity.getProjectKey(), entity.getCreatedAt());
    }
}
