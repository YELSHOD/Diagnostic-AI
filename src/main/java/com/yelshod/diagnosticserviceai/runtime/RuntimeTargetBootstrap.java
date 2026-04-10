package com.yelshod.diagnosticserviceai.runtime;

import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RuntimeTargetBootstrap implements ApplicationRunner {

    private final RuntimeTargetRepository runtimeTargetRepository;
    private final AppProperties appProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (runtimeTargetRepository.count() > 0 || appProperties.runtime() == null || appProperties.runtime().defaultLocalTargets() == null) {
            return;
        }

        List<RuntimeTargetEntity> seeds = appProperties.runtime().defaultLocalTargets().stream()
                .map(this::toEntity)
                .toList();
        runtimeTargetRepository.saveAll(seeds);
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
}
