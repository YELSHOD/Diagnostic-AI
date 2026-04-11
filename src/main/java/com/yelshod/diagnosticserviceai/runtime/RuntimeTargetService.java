package com.yelshod.diagnosticserviceai.runtime;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RuntimeTargetService {

    private final List<RuntimeTargetDiscoveryService> discoveryServices;
    private final RuntimeTargetRepository runtimeTargetRepository;
    private final Clock clock;

    public List<RuntimeTargetDto> listRuntimeTargets() {
        return discoveryServices.stream()
                .flatMap(service -> service.discover().stream())
                .sorted(Comparator.comparing(RuntimeTargetDto::id))
                .toList();
    }

    public RuntimeTargetDto findRequiredTarget(String runtimeTargetId) {
        return listRuntimeTargets().stream()
                .filter(target -> target.id().equals(runtimeTargetId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Runtime target not found: " + runtimeTargetId));
    }

    public RuntimeTargetDto createLocalService(UpsertRuntimeTargetRequest request) {
        Instant now = clock.instant();
        RuntimeTargetEntity entity = RuntimeTargetEntity.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .type(RuntimeTargetType.LOCAL_SERVICE)
                .host(request.host())
                .port(request.port())
                .healthUrl(request.healthUrl())
                .logSourceType(request.logSourceType())
                .logSourceRef(request.logSourceRef())
                .enabled(request.enabled())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return toDto(runtimeTargetRepository.save(entity));
    }

    public RuntimeTargetDto updateLocalService(String runtimeTargetId, UpsertRuntimeTargetRequest request) {
        RuntimeTargetEntity entity = loadLocalService(runtimeTargetId);
        entity.setName(request.name());
        entity.setHost(request.host());
        entity.setPort(request.port());
        entity.setHealthUrl(request.healthUrl());
        entity.setLogSourceType(request.logSourceType());
        entity.setLogSourceRef(request.logSourceRef());
        entity.setEnabled(request.enabled());
        entity.setUpdatedAt(clock.instant());
        return toDto(runtimeTargetRepository.save(entity));
    }

    public void deleteLocalService(String runtimeTargetId) {
        RuntimeTargetEntity entity = loadLocalService(runtimeTargetId);
        runtimeTargetRepository.deleteById(entity.getId());
    }

    private RuntimeTargetEntity loadLocalService(String runtimeTargetId) {
        RuntimeTargetEntity entity = runtimeTargetRepository.findById(parseUuid(runtimeTargetId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Runtime target not found: " + runtimeTargetId));
        if (entity.getType() != RuntimeTargetType.LOCAL_SERVICE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only local service targets can be modified");
        }
        return entity;
    }

    private UUID parseUuid(String runtimeTargetId) {
        try {
            return UUID.fromString(runtimeTargetId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Runtime target not found: " + runtimeTargetId);
        }
    }

    private RuntimeTargetDto toDto(RuntimeTargetEntity entity) {
        return new RuntimeTargetDto(
                entity.getId().toString(),
                entity.getName(),
                entity.getType(),
                RuntimeTargetStatus.UNKNOWN,
                entity.getHost(),
                entity.getPort(),
                entity.getHealthUrl(),
                entity.getLogSourceType(),
                entity.getLogSourceRef(),
                Map.of("source", "database"));
    }
}
