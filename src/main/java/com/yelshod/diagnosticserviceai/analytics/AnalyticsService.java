package com.yelshod.diagnosticserviceai.analytics;

import com.yelshod.diagnosticserviceai.persistence.repository.IncidentRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final IncidentRepository incidentRepository;

    public AnalyticsResponse getAnalytics(Instant from, Instant to, String service) {
        var errors = incidentRepository.errorsPerMinute(from, to, service).stream()
                .map(row -> new AnalyticsResponse.ErrorsPerMinutePoint(row.getBucket(), row.getCount()))
                .toList();

        var topExceptions = incidentRepository.topExceptionTypes(from, to, service).stream()
                .map(row -> new AnalyticsResponse.TopItem(row.getExceptionType(), row.getCount()))
                .toList();

        var topClusters = incidentRepository.topClusters(from, to, service).stream()
                .map(row -> new AnalyticsResponse.TopItem(row.getClusterKey(), row.getCount()))
                .toList();

        return new AnalyticsResponse(errors, topExceptions, topClusters);
    }
}
