package com.yelshod.diagnosticserviceai.ai;

import com.yelshod.diagnosticserviceai.persistence.entity.AiDiagnosisEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.AiDiagnosisRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiDiagnosisPersistenceService {

    private final AiDiagnosisRepository aiDiagnosisRepository;

    public void save(String clusterKey, String model, String promptVersion, String diagnosisJson) {
        aiDiagnosisRepository.save(AiDiagnosisEntity.builder()
                .clusterKey(clusterKey)
                .model(model)
                .promptVersion(promptVersion)
                .diagnosisJson(diagnosisJson)
                .createdAt(Instant.now())
                .build());
    }
}
