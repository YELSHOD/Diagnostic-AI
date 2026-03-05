package com.yelshod.diagnosticserviceai.analytics;

import java.time.Instant;

public interface ErrorsPerMinuteRow {
    Instant getBucket();
    long getCount();
}
