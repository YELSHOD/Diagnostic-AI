package com.yelshod.diagnosticserviceai.persistence.repository;

import com.yelshod.diagnosticserviceai.persistence.entity.AiDiagnosisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiDiagnosisRepository extends JpaRepository<AiDiagnosisEntity, String> {
}
