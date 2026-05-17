package com.yelshod.diagnosticserviceai.runtime;

import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuntimeTargetService {

    private final List<RuntimeTargetDiscoveryService> discoveryServices;
    private final RuntimeTargetRepository runtimeTargetRepository;
    private final Clock clock;

    public List<RuntimeTargetDto> listRuntimeTargets() {
        List<RuntimeTargetDto> targets = discoveryServices.stream()
                .flatMap(service -> service.discover().stream())
                .sorted(Comparator.comparing(RuntimeTargetDto::id))
                .toList();
        log.debug("Runtime targets listed count={}", targets.size());
        return targets;
    }

    public RuntimeTargetDto findRequiredTarget(String runtimeTargetId) {
        return listRuntimeTargets().stream()
                .filter(target -> target.id().equals(runtimeTargetId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Runtime target lookup failed id={}", runtimeTargetId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Runtime target not found: " + runtimeTargetId);
                });
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
        RuntimeTargetEntity savedEntity = runtimeTargetRepository.save(entity);
        log.info("Runtime target created id={} name={} type={}",
                savedEntity.getId(), savedEntity.getName(), savedEntity.getType());
        return toDto(savedEntity);
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
        RuntimeTargetEntity savedEntity = runtimeTargetRepository.save(entity);
        log.info("Runtime target updated id={} name={} enabled={}",
                savedEntity.getId(), savedEntity.getName(), savedEntity.isEnabled());
        return toDto(savedEntity);
    }

    public void deleteLocalService(String runtimeTargetId) {
        RuntimeTargetEntity entity = loadLocalService(runtimeTargetId);
        runtimeTargetRepository.deleteById(entity.getId());
        log.info("Runtime target deleted id={} name={}", entity.getId(), entity.getName());
    }

    private RuntimeTargetEntity loadLocalService(String runtimeTargetId) {
        RuntimeTargetEntity entity = runtimeTargetRepository.findById(parseUuid(runtimeTargetId))
                .orElseThrow(() -> {
                    log.warn("Runtime target lookup failed id={}", runtimeTargetId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Runtime target not found: " + runtimeTargetId);
                });
        if (entity.getType() != RuntimeTargetType.LOCAL_SERVICE) {
            log.warn("Runtime target modification rejected id={} type={}", runtimeTargetId, entity.getType());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only local service targets can be modified");
        }
        return entity;
    }

    private UUID parseUuid(String runtimeTargetId) {
        try {
            return UUID.fromString(runtimeTargetId);
        } catch (IllegalArgumentException ex) {
            log.warn("Runtime target id rejected invalid-format id={}", runtimeTargetId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Runtime target not found: " + runtimeTargetId);
        }
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
                        : RuntimeTargetStatus.UNKNOWN,
                entity.getHost(),
                entity.getPort(),
                entity.getHealthUrl(),
                entity.getLogSourceType(),
                entity.getLogSourceRef(),
                metadata);
    }
}
