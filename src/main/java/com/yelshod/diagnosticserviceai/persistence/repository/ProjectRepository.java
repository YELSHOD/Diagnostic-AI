package com.yelshod.diagnosticserviceai.persistence.repository;

import com.yelshod.diagnosticserviceai.persistence.entity.ProjectEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

    List<ProjectEntity> findAllByOrderByCreatedAtDesc();

    Optional<ProjectEntity> findFirstByOrderByCreatedAtAsc();

    Optional<ProjectEntity> findByProjectKey(String projectKey);

    boolean existsByProjectKey(String projectKey);
}
