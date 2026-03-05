package com.yelshod.diagnosticserviceai.api;

import com.yelshod.diagnosticserviceai.analytics.AnalyticsResponse;
import com.yelshod.diagnosticserviceai.analytics.AnalyticsService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public AnalyticsResponse analytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String service
    ) {
        Instant toTs = to == null ? Instant.now() : to;
        Instant fromTs = from == null ? toTs.minus(1, ChronoUnit.HOURS) : from;
        return analyticsService.getAnalytics(fromTs, toTs, service);
    }
}
