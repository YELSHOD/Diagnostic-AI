package com.yelshod.diagnosticserviceai.runtime;

import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RuntimeTargetBootstrap implements ApplicationRunner {

    private final RuntimeTargetRepository runtimeTargetRepository;
    private final AppProperties appProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (appProperties.runtime() == null || appProperties.runtime().defaultLocalTargets() == null) {
            log.debug("Runtime target bootstrap skipped reason=no-configured-defaults");
            return;
        }

        log.info("Runtime target bootstrap started configuredTargets={}",
                appProperties.runtime().defaultLocalTargets().size());
        List<RuntimeTargetEntity> seeds = appProperties.runtime().defaultLocalTargets().stream()
                .map(target -> runtimeTargetRepository.findByName(target.name())
                        .map(entity -> updateEntity(entity, target))
                        .orElseGet(() -> toEntity(target)))
                .toList();
        if (seeds.isEmpty()) {
            log.debug("Runtime target bootstrap skipped reason=all-defaults-exist");
            return;
        }
        runtimeTargetRepository.saveAll(seeds);
        log.info("Runtime target bootstrap upserted targets count={}", seeds.size());
    }

    private RuntimeTargetEntity toEntity(AppProperties.LocalTarget target) {
        Instant now = Instant.now();
        return RuntimeTargetEntity.builder()
                .id(UUID.randomUUID())
                .name(target.name())
                .type(RuntimeTargetType.LOCAL_SERVICE)
                .host(target.host())
                .port(target.port())
                .healthUrl(target.healthUrl())
                .logSourceType(LogSourceType.valueOf(target.logSourceType().toUpperCase(Locale.ROOT)))
                .logSourceRef(target.logSourceRef())
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private RuntimeTargetEntity updateEntity(RuntimeTargetEntity entity, AppProperties.LocalTarget target) {
        entity.setType(RuntimeTargetType.LOCAL_SERVICE);
        entity.setHost(target.host());
        entity.setPort(target.port());
        entity.setHealthUrl(target.healthUrl());
        entity.setLogSourceType(LogSourceType.valueOf(target.logSourceType().toUpperCase(Locale.ROOT)));
        entity.setLogSourceRef(target.logSourceRef());
        entity.setEnabled(true);
        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
