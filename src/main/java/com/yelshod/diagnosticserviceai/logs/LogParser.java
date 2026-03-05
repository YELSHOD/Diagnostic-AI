package com.yelshod.diagnosticserviceai.logs;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LogParser {

    private static final Pattern LEVEL_PATTERN = Pattern.compile("\\b(INFO|WARN|ERROR|DEBUG)\\b");
    private static final Pattern TRACE_PATTERN = Pattern.compile("(?i)\\btrace[-_ ]?id[=: ]([a-zA-Z0-9-]+)");
    private static final Pattern ISO_TS_PATTERN = Pattern.compile(
            "^\\s*(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2}))");

    public ParsedLogLine parse(String service, String rawLine) {
        String line = rawLine == null ? "" : rawLine.stripTrailing();
        Matcher levelMatcher = LEVEL_PATTERN.matcher(line);
        String level = levelMatcher.find() ? levelMatcher.group(1) : null;

        Matcher traceMatcher = TRACE_PATTERN.matcher(line);
        String traceId = traceMatcher.find() ? traceMatcher.group(1) : null;

        Instant ts = null;
        Matcher tsMatcher = ISO_TS_PATTERN.matcher(line);
        if (tsMatcher.find()) {
            try {
                ts = OffsetDateTime.parse(tsMatcher.group(1)).toInstant();
            } catch (DateTimeParseException ignored) {
            }
        }

        return new ParsedLogLine(ts, level, line, traceId, service, line);
    }
}
