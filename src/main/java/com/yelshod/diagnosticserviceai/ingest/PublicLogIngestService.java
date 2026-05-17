package com.yelshod.diagnosticserviceai.ingest;

import com.yelshod.diagnosticserviceai.cluster.ClusterResult;
import com.yelshod.diagnosticserviceai.cluster.ClusterService;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import com.yelshod.diagnosticserviceai.persistence.entity.LogEventEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.ProjectEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.LogEventRepository;
import com.yelshod.diagnosticserviceai.persistence.repository.ProjectRepository;
import com.yelshod.diagnosticserviceai.runtime.LogSourceType;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetEntity;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetRepository;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetType;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class PublicLogIngestService {

    private static final Pattern EXCEPTION_PATTERN = Pattern.compile("([\\w.$]+(?:Exception|Error))");

    private final ProjectRepository projectRepository;
    private final RuntimeTargetRepository runtimeTargetRepository;
    private final LogEventRepository logEventRepository;
    private final ClusterService clusterService;
    private final Clock clock;

    @Transactional
    public LogIngestResponse ingest(LogIngestRequest request) {
        ProjectEntity project = projectRepository.findByProjectKey(request.projectKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid project key"));
        RuntimeTargetEntity target = findOrCreateRuntimeTarget(project, request);
        Instant eventTime = request.timestamp() == null ? clock.instant() : request.timestamp();
        saveLogEvent(project, target, request, eventTime);

        if (!"ERROR".equals(request.level())) {
            log.debug("Public log ingested level={} project={} service={}",
                    request.level(), project.getId(), request.serviceName());
            return new LogIngestResponse("accepted", target.getId().toString(), null, false);
        }

        ErrorEvent event = toErrorEvent(request, eventTime);
        ClusterResult cluster = clusterService.processEvent(event, project.getId());
        log.info("Public error ingested project={} service={} clusterKey={} count={}",
                project.getId(), request.serviceName(), cluster.clusterKey(), cluster.count());
        return new LogIngestResponse("accepted", target.getId().toString(), cluster.clusterKey(), true);
    }

    private void saveLogEvent(ProjectEntity project, RuntimeTargetEntity target, LogIngestRequest request, Instant eventTime) {
        logEventRepository.save(LogEventEntity.builder()
                .id(UUID.randomUUID())
                .projectId(project.getId())
                .runtimeTargetId(target.getId())
                .service(request.serviceName().trim())
                .level(request.level())
                .message(request.message().trim())
                .stacktrace(request.stackTrace())
                .environment(request.environment())
                .eventTime(eventTime)
                .createdAt(clock.instant())
                .build());
    }

    private RuntimeTargetEntity findOrCreateRuntimeTarget(ProjectEntity project, LogIngestRequest request) {
        String serviceName = request.serviceName().trim();
        return runtimeTargetRepository.findByProjectIdAndName(project.getId(), serviceName)
                .orElseGet(() -> {
                    Instant now = clock.instant();
                    RuntimeTargetEntity entity = RuntimeTargetEntity.builder()
                            .id(UUID.randomUUID())
                            .projectId(project.getId())
                            .name(serviceName)
                            .type(RuntimeTargetType.LOCAL_SERVICE)
                            .host(request.environment())
                            .port(null)
                            .healthUrl(null)
                            .logSourceType(LogSourceType.HTTP_INGEST)
                            .logSourceRef("project:" + project.getId())
                            .enabled(true)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    RuntimeTargetEntity saved = runtimeTargetRepository.save(entity);
                    log.info("Runtime target auto-created from public ingest project={} service={} targetId={}",
                            project.getId(), serviceName, saved.getId());
                    return saved;
                });
    }

    private ErrorEvent toErrorEvent(LogIngestRequest request, Instant eventTime) {
        String stackTrace = request.stackTrace() == null ? "" : request.stackTrace().strip();
        List<String> stackLines = stackTrace.isBlank()
                ? List.of()
                : Arrays.stream(stackTrace.split("\\R"))
                        .map(String::stripTrailing)
                        .filter(line -> !line.isBlank())
                        .limit(80)
                        .toList();
        String exceptionType = extractExceptionType(request.message(), stackTrace);
        String firstStackLine = firstStackTraceLine(stackLines);
        List<String> topFrames = firstStackLine == null ? List.of() : List.of(firstStackLine);
        String context = "environment=" + safe(request.environment()) + " level=" + request.level();
        return new ErrorEvent(
                request.serviceName().trim(),
                eventTime,
                null,
                exceptionType,
                request.message().trim(),
                topFrames,
                stackTrace.isBlank() ? request.message().trim() : request.message().trim() + "\n" + stackTrace,
                List.of(context, request.message().trim())
        );
    }

    private String extractExceptionType(String message, String stackTrace) {
        Matcher stackMatcher = EXCEPTION_PATTERN.matcher(stackTrace == null ? "" : stackTrace);
        if (stackMatcher.find()) {
            return stackMatcher.group(1);
        }
        Matcher messageMatcher = EXCEPTION_PATTERN.matcher(message == null ? "" : message);
        if (messageMatcher.find()) {
            return messageMatcher.group(1);
        }
        return "UnknownException";
    }

    private String firstStackTraceLine(List<String> stackLines) {
        return stackLines.stream()
                .filter(line -> line.stripLeading().startsWith("at ") || line.contains("Exception") || line.contains("Error"))
                .findFirst()
                .orElse(stackLines.isEmpty() ? null : stackLines.getFirst());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
