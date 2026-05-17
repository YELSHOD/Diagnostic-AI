package com.yelshod.diagnosticserviceai.runtime;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfiguredLocalServiceDiscoveryService implements RuntimeTargetDiscoveryService {

    private final RuntimeTargetRepository runtimeTargetRepository;
    private final RuntimeStatusProbe runtimeStatusProbe;

    @Override
    public List<RuntimeTargetDto> discover() {
        return runtimeTargetRepository.findAllByEnabledTrueOrderByNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    private RuntimeTargetDto toDto(RuntimeTargetEntity entity) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "database");
        if (entity.getProjectId() != null) {
            metadata.put("projectId", entity.getProjectId().toString());
        }
        return new RuntimeTargetDto(
                entity.getId().toString(),
                entity.getName(),
                entity.getType(),
                entity.getLogSourceType() == LogSourceType.HTTP_INGEST
                        ? RuntimeTargetStatus.UP
                        : runtimeStatusProbe.probe(entity.getHealthUrl()),
                entity.getHost(),
                entity.getPort(),
                entity.getHealthUrl(),
                entity.getLogSourceType(),
                entity.getLogSourceRef(),
                metadata
        );
    }
}
