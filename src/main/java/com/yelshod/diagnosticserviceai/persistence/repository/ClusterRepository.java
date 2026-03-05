package com.yelshod.diagnosticserviceai.persistence.repository;

import com.yelshod.diagnosticserviceai.persistence.entity.ClusterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClusterRepository extends JpaRepository<ClusterEntity, String> {
}
