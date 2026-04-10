package com.yelshod.diagnosticserviceai.runtime;

import java.util.List;
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
        return new RuntimeTargetDto(
                entity.getId().toString(),
                entity.getName(),
                entity.getType(),
                runtimeStatusProbe.probe(entity.getHealthUrl()),
                entity.getHost(),
                entity.getPort(),
                entity.getHealthUrl(),
                entity.getLogSourceType(),
                entity.getLogSourceRef(),
                Map.of("source", "database")
        );
    }
}
