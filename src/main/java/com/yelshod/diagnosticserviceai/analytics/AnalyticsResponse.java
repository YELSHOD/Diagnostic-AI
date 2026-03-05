package com.yelshod.diagnosticserviceai.analytics;

import java.time.Instant;
import java.util.List;

public record AnalyticsResponse(
        List<ErrorsPerMinutePoint> errorsPerMinute,
        List<TopItem> topExceptionTypes,
        List<TopItem> topClusters
) {

    public record ErrorsPerMinutePoint(Instant bucket, long count) {
    }

    public record TopItem(String key, long count) {
    }
}
